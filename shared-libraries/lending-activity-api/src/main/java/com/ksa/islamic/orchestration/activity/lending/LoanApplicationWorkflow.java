package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;

/**
 * Temporal workflow for loan application processing with SAGA compensation.
 *
 * <p>Orchestrates the complete loan origination flow:</p>
 * <ol>
 *   <li>Create application (DRAFT)</li>
 *   <li>Submit application (SUBMITTED)</li>
 *   <li>Document verification (DOCUMENTS_PENDING → UNDER_REVIEW)</li>
 *   <li>Credit check via credit-service (CREDIT_CHECK)</li>
 *   <li>Sharia compliance validation (SHARIA_VALIDATION)</li>
 *   <li>Profit calculation (Murabaha/Ijara/Tawarruq)</li>
 *   <li>Approval decision (PENDING_APPROVAL → APPROVED/REJECTED)</li>
 *   <li>Loan creation from approved application</li>
 * </ol>
 *
 * <p>SAGA compensation: On failure at any step, previous steps are compensated
 * (e.g., application cancelled, loan reversed).</p>
 */
@WorkflowInterface
public interface LoanApplicationWorkflow {

    @WorkflowMethod
    LoanApplicationResult execute(LoanApplicationRequest request);

    @SignalMethod
    void documentsVerified(DocumentsVerifiedSignal signal);

    @SignalMethod
    void manualApproval(ManualApprovalSignal signal);

    @QueryMethod
    String getCurrentStage();

    @QueryMethod
    String getApplicationStatus();

    // ==================== REQUEST / RESULT ====================

    record LoanApplicationRequest(
            String tenantId,
            String customerId,
            String productId,
            String productCode,
            String shariaStructure,
            BigDecimal requestedAmount,
            int requestedTenureMonths,
            String partnerId,
            String leadId,
            String createdBy
    ) {}

    record LoanApplicationResult(
            String workflowId,
            String applicationId,
            String applicationNumber,
            String loanId,
            String loanNumber,
            String status,
            String failureReason
    ) {}

    record DocumentsVerifiedSignal(
            boolean verified,
            String verifiedBy
    ) {}

    record ManualApprovalSignal(
            boolean approved,
            String approvedBy,
            BigDecimal approvedAmount,
            int approvedTenureMonths,
            BigDecimal approvedProfitRate,
            String reason
    ) {}
}
