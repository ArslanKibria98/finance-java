package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Temporal activity for manual approval gate in loan origination.
 * Calls product-service approval-decision endpoint and manages manual_approval_tasks.
 */
@ActivityInterface
public interface ManualApprovalActivity {

    /**
     * Evaluates product approval rules for this application.
     * Returns AUTO_APPROVAL, MANUAL_APPROVAL, or REJECTION_SCENARIO.
     */
    @ActivityMethod
    ApprovalDecisionResult evaluateApproval(EvaluateInput input);

    /**
     * Creates a PENDING manual_approval_task with SLA deadline computed from
     * product_duration_settings.approval_duration_days.
     */
    @ActivityMethod
    CreateTaskResult createManualApprovalTask(CreateTaskInput input);

    /**
     * Marks the task as BREACHED when SLA timer fires (task still actionable).
     */
    @ActivityMethod
    void markSlaBreached(MarkBreachedInput input);

    /**
     * Emits financing.loan.approval.sla-breached Kafka event.
     */
    @ActivityMethod
    void publishSlaBreachEvent(SlaBreachEventInput input);

    // ── INPUT / OUTPUT RECORDS ──

    record EvaluateInput(
            String tenantId,
            String productId,
            BigDecimal loanAmount,
            Integer creditScore,
            BigDecimal dbrPercentage,
            BigDecimal monthlySalary
    ) {}

    record ApprovalDecisionResult(
            String decision,       // AUTO_APPROVAL | MANUAL_APPROVAL | REJECTION_SCENARIO
            String workflowId,
            String workflowName,
            int slaDays            // product_duration_settings.approval_duration_days
    ) {}

    record CreateTaskInput(
            String tenantId,
            String applicationId,
            String applicationNumber,
            String customerId,
            String customerName,
            String productId,
            String productName,
            BigDecimal requestedAmount,
            int tenureMonths,
            BigDecimal monthlyInstallment,
            Integer creditScore,
            BigDecimal dbrPercentage,
            String assignedRole,
            int slaDays,
            String workflowId
    ) {}

    record CreateTaskResult(
            String taskId,
            String slaDeadline
    ) {}

    record MarkBreachedInput(
            String tenantId,
            String taskId
    ) {}

    record SlaBreachEventInput(
            String tenantId,
            String taskId,
            String applicationId,
            String applicationNumber,
            String customerId,
            BigDecimal requestedAmount,
            String assignedRole
    ) {}
}
