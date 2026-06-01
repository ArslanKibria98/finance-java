package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.domain.model.ManualApprovalTask;
import com.ksa.financing.lending.infrastructure.persistence.entity.ManualApprovalTaskJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaManualApprovalTaskRepository;
import com.ksa.islamic.orchestration.activity.lending.ManualApprovalActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ManualApprovalActivityImpl implements ManualApprovalActivity {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JpaManualApprovalTaskRepository taskRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String productServiceUrl;
    private final String slaBreachTopic;
    private final String approvalRequiredTopic;

    public ManualApprovalActivityImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            JpaManualApprovalTaskRepository taskRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.services.product-service-url}") String productServiceUrl,
            @Value("${kafka.topics.approval-sla-breached:financing.loan.approval.sla-breached}") String slaBreachTopic,
            @Value("${kafka.topics.approval-required:financing.loan.approval.required}") String approvalRequiredTopic) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.taskRepository = taskRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.productServiceUrl = productServiceUrl;
        this.slaBreachTopic = slaBreachTopic;
        this.approvalRequiredTopic = approvalRequiredTopic;
    }

    @Override
    public ApprovalDecisionResult evaluateApproval(EvaluateInput input) {
        log.info("Activity: Evaluating approval for product={} amount={} tenant={}",
                input.productId(), input.loanAmount(), input.tenantId());

        try {
            var builder = UriComponentsBuilder.fromHttpUrl(productServiceUrl)
                    .pathSegment("api", "v1", "products", input.productId(), "approval-decision")
                    .queryParam("tenantId", input.tenantId());

            if (input.loanAmount() != null)    builder.queryParam("loanAmount", input.loanAmount());
            if (input.creditScore() != null)   builder.queryParam("creditScore", input.creditScore());
            if (input.dbrPercentage() != null) builder.queryParam("dbrPercentage", input.dbrPercentage());
            if (input.monthlySalary() != null) builder.queryParam("monthlySalary", input.monthlySalary());

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") && root.get("data").isObject() ? root.get("data") : root;

            String decision = data.path("decision").asText("MANUAL_APPROVAL");
            String wfId = data.path("workflowId").asText(null);
            String wfName = data.path("workflowName").asText(null);

            int slaDays = fetchSlaDays(input.productId(), input.tenantId());

            log.info("Approval decision for product {}: decision={} slaDays={}",
                    input.productId(), decision, slaDays);
            return new ApprovalDecisionResult(decision, wfId, wfName, slaDays);

        } catch (Exception e) {
            log.warn("Approval rule evaluation failed ({}), defaulting to MANUAL_APPROVAL", e.getMessage());
            return new ApprovalDecisionResult("MANUAL_APPROVAL", null, "Default Manual Review", 2);
        }
    }

    @Override
    @Transactional
    public CreateTaskResult createManualApprovalTask(CreateTaskInput input) {
        UUID tenantUuid = UUID.fromString(input.tenantId());
        UUID applicationUuid = UUID.fromString(input.applicationId());

        var existing = taskRepository.findByTenantIdAndApplicationId(tenantUuid, applicationUuid);
        if (existing.isPresent()) {
            var task = existing.get();
            log.info("Manual approval task already exists for application={}, id={}",
                    input.applicationId(), task.getId());
            return new CreateTaskResult(task.getId().toString(), task.getSlaDeadline().toString());
        }

        var task = new ManualApprovalTaskJpaEntity();
        task.setTenantId(tenantUuid);
        task.setApplicationId(applicationUuid);
        task.setApplicationNumber(input.applicationNumber());
        task.setCustomerId(UUID.fromString(input.customerId()));
        task.setCustomerName(input.customerName());
        task.setProductId(UUID.fromString(input.productId()));
        task.setProductName(input.productName());
        task.setRequestedAmount(input.requestedAmount());
        task.setTenureMonths(input.tenureMonths());
        task.setMonthlyInstallment(input.monthlyInstallment());
        task.setCreditScore(input.creditScore());
        task.setDbrPercentage(input.dbrPercentage());
        task.setAssignedRole(input.assignedRole() != null ? input.assignedRole() : "underwriter");
        task.setStatus(ManualApprovalTask.Status.PENDING);
        task.setSlaDeadline(OffsetDateTime.now().plusDays(input.slaDays()));
        task.setSlaBreached(false);
        task.setWorkflowId(input.workflowId());

        task = taskRepository.save(task);
        log.info("Created manual approval task id={} application={} sla={}",
                task.getId(), input.applicationNumber(), task.getSlaDeadline());

        // Publish admin notification event
        try {
            var event = new java.util.HashMap<String, Object>();
            event.put("eventType", "MANUAL_APPROVAL_REQUIRED");
            event.put("tenantId", input.tenantId());
            event.put("taskId", task.getId().toString());
            event.put("applicationId", input.applicationId());
            event.put("applicationNumber", input.applicationNumber());
            event.put("customerId", input.customerId());
            event.put("customerName", input.customerName());
            event.put("productName", input.productName());
            event.put("requestedAmount", input.requestedAmount() != null ? input.requestedAmount().toPlainString() : null);
            event.put("tenureMonths", input.tenureMonths());
            event.put("creditScore", input.creditScore());
            event.put("dbrPercentage", input.dbrPercentage() != null ? input.dbrPercentage().toPlainString() : null);
            event.put("assignedRole", task.getAssignedRole());
            event.put("slaDeadline", task.getSlaDeadline().toString());
            event.put("occurredAt", OffsetDateTime.now().toString());

            kafkaTemplate.send(approvalRequiredTopic, input.applicationId(), event);
            log.info("Published MANUAL_APPROVAL_REQUIRED for task={} application={}",
                    task.getId(), input.applicationNumber());
        } catch (Exception e) {
            log.error("Failed to publish MANUAL_APPROVAL_REQUIRED: {}", e.getMessage(), e);
        }

        return new CreateTaskResult(task.getId().toString(), task.getSlaDeadline().toString());
    }

    @Override
    @Transactional
    public void markSlaBreached(MarkBreachedInput input) {
        UUID tenantUuid = UUID.fromString(input.tenantId());
        UUID taskUuid = UUID.fromString(input.taskId());

        taskRepository.findByTenantIdAndId(tenantUuid, taskUuid).ifPresent(task -> {
            if (task.getStatus() == ManualApprovalTask.Status.PENDING) {
                task.setStatus(ManualApprovalTask.Status.BREACHED);
            }
            task.setSlaBreached(true);
            taskRepository.save(task);
            log.warn("SLA breached for manual approval task id={} application={}",
                    task.getId(), task.getApplicationNumber());
        });
    }

    @Override
    public void publishSlaBreachEvent(SlaBreachEventInput input) {
        try {
            var payload = Map.of(
                    "eventType", "approval.sla-breached",
                    "tenantId", input.tenantId(),
                    "taskId", input.taskId(),
                    "applicationId", input.applicationId(),
                    "applicationNumber", input.applicationNumber(),
                    "customerId", input.customerId(),
                    "requestedAmount", input.requestedAmount() != null ? input.requestedAmount().toPlainString() : "",
                    "assignedRole", input.assignedRole(),
                    "occurredAt", OffsetDateTime.now().toString()
            );
            kafkaTemplate.send(slaBreachTopic, input.applicationId(), payload);
            log.info("Published SLA breach event: taskId={} topic={}", input.taskId(), slaBreachTopic);
        } catch (Exception e) {
            log.error("Failed to publish SLA breach event: {}", e.getMessage(), e);
        }
    }

    private int fetchSlaDays(String productId, String tenantId) {
        try {
            var url = UriComponentsBuilder.fromHttpUrl(productServiceUrl)
                    .pathSegment("api", "v1", "products", productId, "duration-settings")
                    .queryParam("tenantId", tenantId)
                    .toUriString();
            var headers = new HttpHeaders();
            var resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.has("data") && root.get("data").isObject() ? root.get("data") : root;
            int days = data.path("approvalDurationDays").asInt(2);
            return days > 0 ? days : 2;
        } catch (Exception e) {
            log.debug("Unable to fetch approval SLA days, using default 2: {}", e.getMessage());
            return 2;
        }
    }
}
