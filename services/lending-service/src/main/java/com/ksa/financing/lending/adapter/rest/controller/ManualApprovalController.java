package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.lending.adapter.rest.response.LoanApplicationStepInfo;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;
import com.ksa.financing.lending.domain.model.ManualApprovalTask;
import com.ksa.financing.lending.infrastructure.persistence.entity.ManualApprovalTaskJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaManualApprovalTaskRepository;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.ApplicationStatusInfo;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.ManualReviewDecisionSignal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manual-approvals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manual Approvals", description = "Underwriter review queue for loan applications above auto-approval threshold")
public class ManualApprovalController {

    private final JpaManualApprovalTaskRepository taskRepository;
    private final WorkflowClient workflowClient;

    private static final Set<String> MANUAL_APPROVAL_ALLOWED_FILTERS =
            Set.of("status", "assignedRole", "applicationId", "customerId");
    private static final Set<String> MANUAL_APPROVAL_SEARCHABLE_FIELDS =
            Set.of("applicationNumber", "customerName", "productName", "assignedRole", "status");

    @SecuredEndpoint(obj = "manual-approvals", act = "read")
    @GetMapping
    @Operation(summary = "List manual approval tasks (paginated, filterable, searchable). " +
            "Backwards compatible: ?status=PENDING still works. New: ?search=, ?page=, ?size=")
    public PageResponse<ManualApprovalTaskResponse> list(
            @RequestParam(required = false) String status,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);

        ManualApprovalTask.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ManualApprovalTask.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCodes.BAD_REQUEST,
                        "Invalid status: " + status + ". Allowed: PENDING, APPROVED, REJECTED, BREACHED, EXPIRED");
            }
        }

        ManualApprovalTask.Status finalStatus = statusEnum;
        Specification<ManualApprovalTaskJpaEntity> tenantSpec = (root, q, cb) -> {
            var tenantPred = cb.equal(root.get("tenantId"), tenantId);
            return finalStatus != null ? cb.and(tenantPred, cb.equal(root.get("status"), finalStatus)) : tenantPred;
        };

        Specification<ManualApprovalTaskJpaEntity> dynamic = SpecificationBuilder.<ManualApprovalTaskJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(MANUAL_APPROVAL_ALLOWED_FILTERS)
                .search(query.search())
                .searchableFields(MANUAL_APPROVAL_SEARCHABLE_FIELDS)
                .build();

        var page = taskRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, this::toResponse);
    }

    @SecuredEndpoint(obj = "manual-approvals", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get manual approval task detail")
    public ResponseEntity<ManualApprovalTaskResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        var task = taskRepository.findByTenantIdAndId(tenantId, id)
                .or(() -> taskRepository.findByTenantIdAndApplicationId(tenantId, id))
                .orElseThrow(() -> NotFoundException.forEntity("ManualApprovalTask", id.toString()));
        return ResponseEntity.ok(toResponse(task));
    }

    @SecuredEndpoint(obj = "manual-approvals", act = "update")
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending manual approval task — signals the loan application workflow to continue")
    @Transactional
    public ResponseEntity<ManualApprovalTaskResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID decisionBy = UUID.fromString(jwt.getSubject());
        var task = taskRepository.findByTenantIdAndId(tenantId, id)
                .or(() -> taskRepository.findByTenantIdAndApplicationId(tenantId, id))
                .orElseThrow(() -> NotFoundException.forEntity("ManualApprovalTask", id.toString()));

        if (task.getStatus() != ManualApprovalTask.Status.PENDING
                && task.getStatus() != ManualApprovalTask.Status.BREACHED) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Task is not in a decidable state: " + task.getStatus());
        }

        signalWorkflow(task.getWorkflowId(), true,
                new ManualReviewDecisionSignal(decisionBy.toString(), request.notes(), null));

        task.setStatus(ManualApprovalTask.Status.APPROVED);
        task.setDecisionBy(decisionBy);
        task.setDecisionAt(OffsetDateTime.now());
        task.setDecisionNotes(request.notes());
        taskRepository.save(task);

        var statusInfo = waitForWorkflowStatusUpdate(task.getWorkflowId(), "MANUAL_REVIEW", 10);
        log.info("Manual approval APPROVED: taskId={} by={} application={}",
                id, decisionBy, task.getApplicationNumber());
        return ResponseEntity.ok(toResponse(task, statusInfo));
    }

    @SecuredEndpoint(obj = "manual-approvals", act = "update")
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending manual approval task — signals the loan application workflow to cancel")
    @Transactional
    public ResponseEntity<ManualApprovalTaskResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID decisionBy = UUID.fromString(jwt.getSubject());
        var task = taskRepository.findByTenantIdAndId(tenantId, id)
                .or(() -> taskRepository.findByTenantIdAndApplicationId(tenantId, id))
                .orElseThrow(() -> NotFoundException.forEntity("ManualApprovalTask", id.toString()));

        if (task.getStatus() != ManualApprovalTask.Status.PENDING
                && task.getStatus() != ManualApprovalTask.Status.BREACHED) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Task is not in a decidable state: " + task.getStatus());
        }

        signalWorkflow(task.getWorkflowId(), false,
                new ManualReviewDecisionSignal(decisionBy.toString(), request.notes(), request.rejectionReason()));

        task.setStatus(ManualApprovalTask.Status.REJECTED);
        task.setDecisionBy(decisionBy);
        task.setDecisionAt(OffsetDateTime.now());
        task.setDecisionNotes(request.notes());
        task.setRejectionReason(request.rejectionReason());
        taskRepository.save(task);

        var statusInfo = waitForWorkflowStatusUpdate(task.getWorkflowId(), "MANUAL_REVIEW", 10);
        log.info("Manual approval REJECTED: taskId={} by={} reason={} application={}",
                id, decisionBy, request.rejectionReason(), task.getApplicationNumber());
        return ResponseEntity.ok(toResponse(task, statusInfo));
    }

    private void signalWorkflow(String workflowId, boolean approve, ManualReviewDecisionSignal signal) {
        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            if (approve) {
                workflow.approveManualReview(signal);
            } else {
                workflow.rejectManualReview(signal);
            }
        } catch (WorkflowNotFoundException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Loan application workflow has already completed or expired: " + workflowId);
        }
    }

    private ApplicationStatusInfo waitForWorkflowStatusUpdate(String workflowId, String waitWhileStatus, int maxWaitSeconds) {
        ApplicationStatusInfo statusInfo = null;
        for (int i = 0; i < maxWaitSeconds * 2; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            statusInfo = queryWorkflowStatusSafe(workflowId);
            if (statusInfo == null || statusInfo.status() == null) {
                continue;
            }

            var current = statusInfo.status();
            if ("APPROVED".equals(current) || "REJECTED".equals(current)
                    || "CANCELLED".equals(current) || "EXPIRED".equals(current)) {
                break;
            }
            if (!waitWhileStatus.equals(current)) {
                break;
            }
        }
        return statusInfo;
    }

    private ApplicationStatusInfo queryWorkflowStatusSafe(String workflowId) {
        try {
            var workflow = workflowClient.newWorkflowStub(LoanApplicationWorkflow.class, workflowId);
            return workflow.getApplicationStatus();
        } catch (Exception e) {
            log.debug("Could not query workflow status for {}: {}", workflowId, e.getMessage());
            return null;
        }
    }

    private static String resolveOverallStatus(String rawStatus) {
        if (rawStatus == null) {
            return "active";
        }
        return switch (rawStatus) {
            case "MANUAL_REVIEW" -> "manual_review";
            case "APPROVED" -> "approved";
            case "REJECTED" -> "rejected";
            case "CANCELLED" -> "cancelled";
            case "EXPIRED" -> "expired";
            case "EXPIRED_RESUMABLE" -> "expired_resumable";
            default -> "active";
        };
    }

    private ManualApprovalTaskResponse toResponse(ManualApprovalTaskJpaEntity t) {
        return toResponse(t, null);
    }

    private ManualApprovalTaskResponse toResponse(ManualApprovalTaskJpaEntity t, ApplicationStatusInfo statusInfo) {
        var applicationStatus = statusInfo != null ? statusInfo.status() : null;
        var nextAction = LoanApplicationStepInfo.getNextAction(applicationStatus);
        var workflowOverallStatus = resolveOverallStatus(applicationStatus);

        return new ManualApprovalTaskResponse(
                t.getId(),
                t.getApplicationId(),
                t.getApplicationNumber(),
                t.getCustomerId(),
                t.getCustomerName(),
                t.getProductId(),
                t.getProductName(),
                t.getRequestedAmount(),
                t.getTenureMonths(),
                t.getMonthlyInstallment(),
                t.getCreditScore(),
                t.getDbrPercentage(),
                t.getAssignedRole(),
                t.getStatus().name(),
                t.getSlaDeadline(),
                t.isSlaBreached(),
                t.getDecisionBy(),
                t.getDecisionAt(),
                t.getDecisionNotes(),
                t.getRejectionReason(),
                t.getCreatedAt(),
                applicationStatus,
                workflowOverallStatus,
                nextAction
        );
    }

    private UUID extractTenantId(Jwt jwt) {
        var claim = jwt.getClaimAsString("tenant_id");
        if (claim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "No tenant_id claim found in JWT");
        }
        return UUID.fromString(claim);
    }

    public record ApproveRequest(String notes) {}

    public record RejectRequest(
            @NotBlank String rejectionReason,
            String notes
    ) {}

    public record ManualApprovalTaskResponse(
            UUID id,
            UUID applicationId,
            String applicationNumber,
            UUID customerId,
            String customerName,
            UUID productId,
            String productName,
            BigDecimal requestedAmount,
            int tenureMonths,
            BigDecimal monthlyInstallment,
            Integer creditScore,
            BigDecimal dbrPercentage,
            String assignedRole,
            String status,
            OffsetDateTime slaDeadline,
            boolean slaBreached,
            UUID decisionBy,
            OffsetDateTime decisionAt,
            String decisionNotes,
            String rejectionReason,
            OffsetDateTime createdAt,
            String applicationStatus,
            String workflowStatus,
            String nextAction
    ) {}
}
