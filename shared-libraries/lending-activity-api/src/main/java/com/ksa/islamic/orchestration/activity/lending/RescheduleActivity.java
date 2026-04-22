package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Temporal activity contract for loan rescheduling operations.
 *
 * Implemented in lending-service/adapter/temporal/activity/RescheduleActivityImpl.java
 *
 * All Fineract interactions go through ledger-service (per architecture mandate):
 * - rescheduleInFineract() → calls ledger-service /api/v1/fineract-proxy/reschedule-loans
 * - approveInFineract()    → calls ledger-service /api/v1/fineract-proxy/reschedule-loans/{id}/approve
 *
 * GL entries (RESTRUCTURING only) are posted via LedgerActivity.postDisbursementGlEntry()
 * which already calls ledger-service.
 */
@ActivityInterface
public interface RescheduleActivity {

    /**
     * Validate loan eligibility for the requested reschedule type.
     * Checks: loan exists + ACTIVE, loan age, DPD, previous reschedule count, etc.
     */
    @ActivityMethod
    EligibilityResult validateEligibility(EligibilityInput input);

    /**
     * Save reschedule request to lending-service DB with PENDING status.
     */
    @ActivityMethod
    String saveRescheduleRequest(SaveRescheduleInput input);

    /**
     * Generate new amortization schedule for the loan.
     * Called after approval — creates a new schedule_version in amortization_schedules.
     */
    @ActivityMethod
    ScheduleResult generateNewSchedule(GenerateScheduleInput input);

    /**
     * Submit reschedule to Fineract via ledger-service Fineract proxy.
     * Applicable for TENURE_EXTENSION, RESTRUCTURING.
     */
    @ActivityMethod
    FineractRescheduleResult rescheduleInFineract(FineractRescheduleInput input);

    /**
     * Approve the reschedule in Fineract via ledger-service Fineract proxy.
     */
    @ActivityMethod
    void approveFineractReschedule(ApproveFineractInput input);

    /**
     * Mark reschedule as APPLIED in lending-service DB.
     */
    @ActivityMethod
    void markApplied(MarkAppliedInput input);

    /**
     * Mark reschedule as REJECTED in lending-service DB.
     */
    @ActivityMethod
    void markRejected(MarkRejectedInput input);

    /**
     * Mark reschedule as REJECTED by idempotency key (used when eligibility fails before saveRescheduleRequest).
     * Looks up the pre-saved SUBMITTED record by tenantId + idempotencyKey and marks it REJECTED.
     */
    @ActivityMethod
    void markRejectedByIdempotencyKey(MarkRejectedByKeyInput input);

    // ══════════ DTOs ══════════

    record EligibilityInput(
            String tenantId,
            String loanId,
            String rescheduleType,     // RescheduleType name
            Integer extensionMonths,
            Integer holidayMonths,
            String requestedSkipMonth  // ISO date string
    ) {}

    record EligibilityResult(
            boolean eligible,
            String reason,
            int currentTenureMonths,
            BigDecimal currentInstallment,
            BigDecimal outstandingPrincipal,
            int loanAgeMonths,
            int currentDpd,
            int previousSkips
    ) {}

    record SaveRescheduleInput(
            String tenantId,
            String loanId,
            String loanNumber,
            String rescheduleType,
            String requestedBy,
            String justification,
            String requestedSkipMonth,
            Integer extensionMonths,
            Integer holidayMonths,
            BigDecimal newProfitRate,
            BigDecimal writeOffAmount,
            BigDecimal profitWaiverAmount,
            String workflowId,
            String idempotencyKey
    ) {}

    record GenerateScheduleInput(
            String tenantId,
            String loanId,
            String rescheduleId,
            String rescheduleType,
            Integer extensionMonths,
            Integer holidayMonths,
            String skipMonth,
            BigDecimal newProfitRate
    ) {}

    record ScheduleResult(
            int newTenureMonths,
            BigDecimal newInstallmentAmount,
            LocalDate newMaturityDate
    ) {}

    record FineractRescheduleInput(
            String tenantId,
            String loanId,
            String rescheduleType,
            Integer extensionMonths,
            String rescheduleFromDate,
            BigDecimal newProfitRate,
            String idempotencyKey
    ) {}

    record FineractRescheduleResult(
            Long fineractRescheduleId,
            boolean submitted
    ) {}

    record ApproveFineractInput(
            String tenantId,
            Long fineractRescheduleId,
            String approvedOnDate
    ) {}

    record MarkAppliedInput(
            String rescheduleId,
            int newTenureMonths,
            BigDecimal newInstallmentAmount,
            String newMaturityDate,
            Long fineractRescheduleId,
            String glEntryNumber
    ) {}

    record MarkRejectedInput(
            String rescheduleId,
            String rejectionReason,
            String rejectedBy
    ) {}

    record MarkRejectedByKeyInput(
            String tenantId,
            String idempotencyKey,
            String rejectionReason,
            String rejectedBy
    ) {}
}
