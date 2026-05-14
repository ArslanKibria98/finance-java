package com.ksa.financing.lending.adapter.temporal.workflow;

import com.ksa.islamic.orchestration.activity.lending.LedgerActivity;
import com.ksa.islamic.orchestration.activity.lending.LoanRescheduleWorkflow;
import com.ksa.islamic.orchestration.activity.lending.RescheduleActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Loan Rescheduling Temporal Workflow — per Blueprint 17.
 *
 * Flow:
 * 1. Validate loan eligibility (loan age, DPD, previous skips)
 * 2. Save PENDING reschedule request to lending-service DB
 * 3. SKIP_PAYMENT → auto-approve (self-service, no human needed)
 * 4. TENURE_EXTENSION / PAYMENT_HOLIDAY → wait for approve/reject signal (operations_head)
 * 5. RESTRUCTURING → wait for approve/reject signal (credit_committee)
 * 6. On approval → generate new amortization schedule
 * 7. RESTRUCTURING → post GL write-off entries via LedgerActivity → ledger-service → Fineract GL
 * 8. TENURE_EXTENSION / RESTRUCTURING → sync to Fineract via ledger-service Fineract proxy
 * 9. Mark APPLIED in lending-service DB
 *
 * All Fineract communication goes through ledger-service.
 */
public class LoanRescheduleWorkflowImpl implements LoanRescheduleWorkflow {

    private static final Logger log = Workflow.getLogger(LoanRescheduleWorkflowImpl.class);

    private static final Duration APPROVAL_TIMEOUT = Duration.ofDays(3);  // Manager has 3 days to approve/reject

    // ══════════ ACTIVITY STUBS ══════════

    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    private final ActivityOptions fineractOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setMaximumInterval(Duration.ofMinutes(2))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(5)
                    .build())
            .build();

    private final ActivityOptions ledgerOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(5)
                    .build())
            .build();

    private final RescheduleActivity rescheduleActivity =
            Workflow.newActivityStub(RescheduleActivity.class, defaultOptions);

    private final RescheduleActivity fineractRescheduleActivity =
            Workflow.newActivityStub(RescheduleActivity.class, fineractOptions);

    private final LedgerActivity ledgerActivity =
            Workflow.newActivityStub(LedgerActivity.class, ledgerOptions);

    // ══════════ WORKFLOW STATE ══════════

    private String status = "VALIDATING";
    private ApprovalSignal approvalSignal;
    private RejectionSignal rejectionSignal;
    private boolean decisionReceived = false;

    // ══════════ WORKFLOW METHOD ══════════

    @Override
    public RescheduleResult execute(RescheduleRequest request) {
        log.info("LoanRescheduleWorkflow started: loanId={} type={}", request.loanId(), request.rescheduleType());

        String rescheduleId = null;

        try {
            // ── Step 1: Validate Eligibility ──────────────────────────────
            status = "VALIDATING";
            var eligibility = rescheduleActivity.validateEligibility(
                    new RescheduleActivity.EligibilityInput(
                            request.tenantId(),
                            request.loanId(),
                            request.rescheduleType(),
                            request.extensionMonths(),
                            request.holidayMonths(),
                            request.requestedSkipMonth()
                    ));

            if (!eligibility.eligible()) {
                log.warn("Loan not eligible for reschedule: loanId={} reason={}", request.loanId(), eligibility.reason());
                // Mark the pre-saved SUBMITTED record as REJECTED so it won't block future requests
                try {
                    rescheduleActivity.markRejectedByIdempotencyKey(
                            new RescheduleActivity.MarkRejectedByKeyInput(
                                    request.tenantId(),
                                    request.idempotencyKey(),
                                    eligibility.reason(),
                                    "SYSTEM"
                            ));
                } catch (Exception ex) {
                    log.warn("Could not mark pre-saved reschedule as rejected: {}", ex.getMessage());
                }
                return new RescheduleResult(null, "REJECTED", 0, null, null, false, eligibility.reason());
            }

            log.info("Eligibility passed: loanId={} type={}", request.loanId(), request.rescheduleType());

            // ── Step 2: Save PENDING request ─────────────────────────────
            status = "PENDING";
            rescheduleId = rescheduleActivity.saveRescheduleRequest(
                    new RescheduleActivity.SaveRescheduleInput(
                            request.tenantId(),
                            request.loanId(),
                            request.loanNumber(),
                            request.rescheduleType(),
                            request.requestedBy(),
                            request.justification(),
                            request.requestedSkipMonth(),
                            request.extensionMonths(),
                            request.holidayMonths(),
                            request.newProfitRate(),
                            request.writeOffAmount(),
                            request.profitWaiverAmount(),
                            Workflow.getInfo().getWorkflowId(),
                            request.attachmentUrl(),
                            request.idempotencyKey(),
                            eligibility.currentTenureMonths(),
                            eligibility.currentInstallment(),
                            eligibility.currentMaturityDate()
                    ));

            // ── Step 3: Approval gate ─────────────────────────────────────
            boolean needsApproval = requiresApproval(request.rescheduleType());

            if (needsApproval) {
                status = "AWAITING_APPROVAL";
                log.info("Waiting for approval: rescheduleId={} type={}", rescheduleId, request.rescheduleType());

                boolean received = Workflow.await(APPROVAL_TIMEOUT, () -> decisionReceived);

                if (!received) {
                    // Timed out — auto-cancel
                    rescheduleActivity.markRejected(new RescheduleActivity.MarkRejectedInput(
                            rescheduleId,
                            "Approval timeout — no decision within 3 days",
                            "SYSTEM"
                    ));
                    return new RescheduleResult(rescheduleId, "CANCELLED", 0, null, null, false,
                            "No approval decision received within 3 days");
                }

                if (rejectionSignal != null) {
                    status = "REJECTED";
                    rescheduleActivity.markRejected(new RescheduleActivity.MarkRejectedInput(
                            rescheduleId,
                            rejectionSignal.rejectionReason(),
                            rejectionSignal.rejectedBy()
                    ));
                    return new RescheduleResult(rescheduleId, "REJECTED", 0, null, null, false,
                            rejectionSignal.rejectionReason());
                }

                log.info("Reschedule approved: rescheduleId={} by={}", rescheduleId,
                        approvalSignal.approverId());
            }

            status = "PROCESSING";

            // ── Step 4: Generate new amortization schedule ────────────────
            status = "GENERATING_SCHEDULE";
            var scheduleResult = rescheduleActivity.generateNewSchedule(
                    new RescheduleActivity.GenerateScheduleInput(
                            request.tenantId(),
                            request.loanId(),
                            rescheduleId,
                            request.rescheduleType(),
                            request.extensionMonths(),
                            request.holidayMonths(),
                            request.requestedSkipMonth(),
                            request.newProfitRate()
                    ));

            log.info("New schedule generated: newTenure={} newInstallment={} newMaturity={}",
                    scheduleResult.newTenureMonths(),
                    scheduleResult.newInstallmentAmount(),
                    scheduleResult.newMaturityDate());

            // ── Step 5: GL entries for RESTRUCTURING (via LedgerActivity → ledger-service) ──
            String glEntryNumber = null;
            if ("RESTRUCTURING".equals(request.rescheduleType()) && needsGlEntries(request)) {
                status = "POSTING_GL_ENTRIES";

                // Write-off: Dr. Provision for Bad Debts, Cr. Loan Receivable Principal
                if (request.writeOffAmount() != null && request.writeOffAmount().compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        var glResult = ledgerActivity.postRestructuringWriteOffEntry(
                                new LedgerActivity.RestructuringGlInput(
                                        request.tenantId(),
                                        request.loanId(),
                                        request.loanNumber(),
                                        request.writeOffAmount(),
                                        request.profitWaiverAmount(),
                                        Workflow.getInfo().getWorkflowId() + "-gl-writeoff",
                                        request.createdBy()
                                ));
                        glEntryNumber = glResult.entryNumber();
                        log.info("GL write-off entry posted: entryNumber={}", glEntryNumber);
                    } catch (Exception e) {
                        log.warn("GL write-off entry failed (non-blocking): {}", e.getMessage());
                    }
                }
            }

            // ── Step 6: Fineract sync via ledger-service proxy ─────────────
            // TENURE_EXTENSION and RESTRUCTURING need Fineract loan modification
            Long fineractRescheduleId = null;
            if (needsFineractSync(request.rescheduleType())) {
                status = "SYNCING_FINERACT";
                try {
                    var fineractResult = fineractRescheduleActivity.rescheduleInFineract(
                            new RescheduleActivity.FineractRescheduleInput(
                                    request.tenantId(),
                                    request.loanId(),
                                    request.rescheduleType(),
                                    request.extensionMonths(),
                                    LocalDate.now().toString(),
                                    request.newProfitRate(),
                                    request.idempotencyKey() + "-fineract"
                            ));

                    fineractRescheduleId = fineractResult.fineractRescheduleId();

                    if (fineractRescheduleId != null) {
                        fineractRescheduleActivity.approveFineractReschedule(
                                new RescheduleActivity.ApproveFineractInput(
                                        request.tenantId(),
                                        fineractRescheduleId,
                                        LocalDate.now().toString()
                                ));
                        log.info("Fineract reschedule submitted and approved: id={}", fineractRescheduleId);
                    }
                } catch (Exception e) {
                    log.warn("Fineract sync failed (non-blocking, reconciliation will catch): {}", e.getMessage());
                }
            }

            // ── Step 7: Mark APPLIED ──────────────────────────────────────
            status = "APPLYING";
            rescheduleActivity.markApplied(new RescheduleActivity.MarkAppliedInput(
                    rescheduleId,
                    scheduleResult.newTenureMonths(),
                    scheduleResult.newInstallmentAmount(),
                    scheduleResult.newMaturityDate() != null ? scheduleResult.newMaturityDate().toString() : null,
                    fineractRescheduleId,
                    glEntryNumber
            ));

            status = "APPLIED";
            log.info("Reschedule applied successfully: rescheduleId={} loanId={}", rescheduleId, request.loanId());

            return new RescheduleResult(
                    rescheduleId,
                    "APPLIED",
                    scheduleResult.newTenureMonths(),
                    scheduleResult.newInstallmentAmount(),
                    scheduleResult.newMaturityDate() != null ? scheduleResult.newMaturityDate().toString() : null,
                    true,
                    null
            );

        } catch (Exception e) {
            status = "FAILED";
            log.error("Reschedule workflow failed: loanId={} error={}", request.loanId(), e.getMessage(), e);

            if (rescheduleId != null) {
                try {
                    rescheduleActivity.markRejected(new RescheduleActivity.MarkRejectedInput(
                            rescheduleId, "Workflow error: " + e.getMessage(), "SYSTEM"));
                } catch (Exception ex) {
                    log.warn("Failed to mark reschedule as rejected: {}", ex.getMessage());
                }
            }

            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "RESCHEDULE_FAILED");
        }
    }

    // ══════════ SIGNAL HANDLERS ══════════

    @Override
    public void approve(ApprovalSignal signal) {
        log.info("Approval signal received: approverId={} role={}", signal.approverId(), signal.approverRole());
        this.approvalSignal = signal;
        this.decisionReceived = true;
    }

    @Override
    public void reject(RejectionSignal signal) {
        log.info("Rejection signal received: rejectedBy={} reason={}", signal.rejectedBy(), signal.rejectionReason());
        this.rejectionSignal = signal;
        this.decisionReceived = true;
    }

    @Override
    public String getStatus() {
        return status;
    }

    // ══════════ Helpers ══════════

    private boolean requiresApproval(String type) {
        return "TENURE_EXTENSION".equals(type) || "PAYMENT_HOLIDAY".equals(type) || "RESTRUCTURING".equals(type);
    }

    private boolean needsFineractSync(String type) {
        return "TENURE_EXTENSION".equals(type) || "RESTRUCTURING".equals(type);
    }

    private boolean needsGlEntries(RescheduleRequest request) {
        return (request.writeOffAmount() != null && request.writeOffAmount().compareTo(BigDecimal.ZERO) > 0)
                || (request.profitWaiverAmount() != null && request.profitWaiverAmount().compareTo(BigDecimal.ZERO) > 0);
    }
}
