package com.ksa.financing.risk.infrastructure.adapter;

import com.ksa.financing.risk.domain.model.approval.ApprovalWorkflowRule;
import com.ksa.financing.risk.domain.port.out.ApprovalWorkflowPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceApprovalWorkflowAdapter implements ApprovalWorkflowPort {

    private final RestTemplate restTemplate;

    @Value("${app.services.product-service-url:http://localhost:8091}")
    private String productServiceUrl;

    @Override
    @SuppressWarnings("unchecked")
    public List<ApprovalWorkflowRule> fetchWorkflowRules(UUID tenantId, UUID productId, String authToken) {
        try {
            String url = productServiceUrl + "/api/v1/products/" + productId;

            var headers = new HttpHeaders();
            if (authToken != null && !authToken.isBlank()) {
                headers.setBearerAuth(authToken);
            }

            var entity = new HttpEntity<>(headers);
            var responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            var response = responseEntity.getBody();
            if (response == null) return List.of();

            var data = (Map<String, Object>) response.getOrDefault("data", response);
            var workflows = (List<Map<String, Object>>) data.get("approvalWorkflows");
            if (workflows == null || workflows.isEmpty()) return List.of();

            var rules = new ArrayList<ApprovalWorkflowRule>();
            for (var wf : workflows) {
                var active = Boolean.TRUE.equals(wf.get("active"));
                if (!active) continue;

                var conditions = new ArrayList<ApprovalWorkflowRule.Condition>();
                var condList = (List<Map<String, Object>>) wf.getOrDefault("conditions", List.of());
                for (var c : condList) {
                    conditions.add(new ApprovalWorkflowRule.Condition(
                            (String) c.get("field"),
                            (String) c.get("operator"),
                            (String) c.get("value")));
                }

                var actions = (List<Map<String, Object>>) wf.getOrDefault("actions", List.of());
                String actionType = actions.isEmpty() ? "REJECT"
                        : (String) actions.getFirst().getOrDefault("actionType", "REJECT");

                rules.add(new ApprovalWorkflowRule(
                        parseUuid(wf.get("id")),
                        (String) wf.get("workflowType"),
                        (String) wf.get("nameEn"),
                        wf.get("priority") != null ? ((Number) wf.get("priority")).intValue() : 0,
                        true,
                        conditions,
                        actionType));
            }

            rules.sort(Comparator.comparingInt(ApprovalWorkflowRule::priority));
            return rules;

        } catch (Exception e) {
            log.error("Failed to fetch approval workflows from product-service for product={}: {}",
                    productId, e.getMessage());
            return List.of();
        }
    }

    private UUID parseUuid(Object value) {
        if (value == null) return UUID.randomUUID();
        return UUID.fromString(value.toString());
    }
}
