package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;

/**
 * Temporal workflow contract for loan rescheduling.
 *
 * Flow (per Blueprint 17):
 * 1. Validate eligibility
 * 2. Save PENDING request to DB
 * 3. If no approval needed (SKIP_PAYMENT) → proceed directly
 * 4. If approval needed → wait for approve/reject signal (manager/credit committee)
 * 5. If approved → generate new amortization schedule
 * 6. If RESTRUCTURING → post GL write-off entries via LedgerActivity → ledger-service
 * 7. Sync to Fineract via ledger-service proxy (TENURE_EXTENSION, RESTRUCTURING)
 * 8. Mark APPLIED
 */
@WorkflowInterface
public interface LoanRescheduleWorkflow {

    @WorkflowMethod
    RescheduleResult execute(RescheduleRequest request);

    /** Signal: manager/credit committee approves the reschedule */
    @SignalMethod
    void approve(ApprovalSignal signal);

    /** Signal: manager/credit committee rejects the reschedule */
    @SignalMethod
    void reject(RejectionSignal signal);

    @QueryMethod
    String getStatus();

    // ══════════ DTOs ══════════

    record RescheduleRequest(
            String tenantId,
            String loanId,
            String loanNumber,
            String rescheduleType,         // RescheduleType name
            String requestedBy,
            String justification,
            String requestedSkipMonth,     // ISO date — for SKIP_PAYMENT
            Integer extensionMonths,       // for TENURE_EXTENSION
            Integer holidayMonths,         // for PAYMENT_HOLIDAY
            BigDecimal newProfitRate,      // for RESTRUCTURING
            BigDecimal writeOffAmount,     // for RESTRUCTURING
            BigDecimal profitWaiverAmount, // for RESTRUCTURING
            BigDecimal outstandingPrincipal,
            String idempotencyKey,
            String createdBy
    ) {}

    record RescheduleResult(
            String rescheduleId,
            String status,
            int newTenureMonths,
            BigDecimal newInstallmentAmount,
            String newMaturityDate,
            boolean success,
            String errorMessage
    ) {}

    record ApprovalSignal(
            String approverId,
            String approverRole,
            String approvalNotes
    ) {}

    record RejectionSignal(
            String rejectedBy,
            String rejectionReason
    ) {}
}
