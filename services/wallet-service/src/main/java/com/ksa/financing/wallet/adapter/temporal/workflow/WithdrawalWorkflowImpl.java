package com.ksa.financing.wallet.adapter.temporal.workflow;

import com.ksa.financing.wallet.adapter.temporal.activity.WithdrawalActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Async withdrawal SAGA.
 * <p>
 * Step machine:
 *   DEBITED → BANK_SUBMITTED → (signal-await) → COMPLETED
 *                              ↘ (timeout / rejection signal) → COMPENSATED
 *   debit-failure → FAILED (no compensation needed)
 */
public class WithdrawalWorkflowImpl implements WithdrawalWorkflow {

    private static final Logger log = Workflow.getLogger(WithdrawalWorkflowImpl.class);

    private static final RetryOptions FINERACT_RETRY = RetryOptions.newBuilder()
            .setMaximumAttempts(5)
            .setInitialInterval(Duration.ofSeconds(2))
            .setMaximumInterval(Duration.ofSeconds(30))
            .setBackoffCoefficient(2.0)
            .build();

    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(FINERACT_RETRY)
            .build();

    private static final Duration SETTLEMENT_TIMEOUT = Duration.ofHours(24);

    private final WithdrawalActivity activities =
            Workflow.newActivityStub(WithdrawalActivity.class, ACTIVITY_OPTIONS);

    private String status = "INITIATED";
    private String bankReference;
    private String sarieReference;
    private String rejectionCode;
    private String rejectionMessage;
    private boolean settlementSignalled = false;
    private boolean settlementAccepted = false;

    @Override
    public WithdrawalResult execute(WithdrawalInput input) {
        log.info("Starting withdrawal workflow withdrawalId={} number={}",
                input.withdrawalId(), input.withdrawalNumber());

        boolean debited = false;

        try {
            // Step 1: Debit Fineract
            status = "DEBITING";
            String debitTxn = activities.debitFineract(
                    input.fineractSavingsId(), input.totalDebit(),
                    "WDR:" + input.withdrawalNumber());
            debited = true;
            status = "DEBITED";
            activities.markStatus(input.withdrawalId(), "DEBITED", null, null, null, null);
            log.info("Withdrawal DEBITED txn={}", debitTxn);

            // Step 2: Submit to bank rails
            status = "SUBMITTING_BANK";
            WithdrawalActivity.BankRailsResult submission = activities.submitToBankRails(
                    new WithdrawalActivity.BankRailsInput(
                            input.withdrawalNumber(), input.idempotencyKey(),
                            input.beneficiaryName(), input.destinationIban(),
                            null, input.amount(), input.currency(),
                            null, "Wallet withdrawal " + input.withdrawalNumber()));

            if (!submission.accepted()) {
                log.warn("Bank rails REJECTED at submission code={}", submission.rejectionCode());
                return compensate(input, submission.rejectionCode(), submission.rejectionMessage());
            }
            bankReference = submission.bankReference();
            sarieReference = submission.sarieReference();
            status = "BANK_SUBMITTED";
            activities.markStatus(input.withdrawalId(), "BANK_SUBMITTED",
                    bankReference, sarieReference, null, null);

            // Step 3: Wait for async settlement signal (or timeout)
            boolean settled = Workflow.await(SETTLEMENT_TIMEOUT, () -> settlementSignalled);
            if (!settled) {
                log.warn("Settlement timeout — compensating withdrawalId={}", input.withdrawalId());
                return compensate(input, "WALLET.WITHDRAWAL.SETTLEMENT_TIMEOUT",
                        "Bank settlement timed out after " + SETTLEMENT_TIMEOUT);
            }
            if (!settlementAccepted) {
                log.warn("Settlement REJECTED — compensating withdrawalId={}", input.withdrawalId());
                return compensate(input, rejectionCode, rejectionMessage);
            }

            // Step 4: Mark completed
            status = "COMPLETED";
            activities.markStatus(input.withdrawalId(), "COMPLETED",
                    bankReference, sarieReference, null, null);
            log.info("Withdrawal COMPLETED withdrawalId={} bankRef={}",
                    input.withdrawalId(), bankReference);
            return new WithdrawalResult(input.withdrawalId(), "COMPLETED",
                    bankReference, sarieReference, null, null);

        } catch (Exception ex) {
            log.error("Withdrawal workflow failed withdrawalId={} step={}: {}",
                    input.withdrawalId(), status, ex.getMessage());
            if (debited) {
                return compensate(input, "WALLET.WITHDRAWAL.WORKFLOW_FAILED", ex.getMessage());
            }
            status = "FAILED";
            activities.markStatus(input.withdrawalId(), "FAILED", null, null,
                    "WALLET.WITHDRAWAL.WORKFLOW_FAILED", ex.getMessage());
            return new WithdrawalResult(input.withdrawalId(), "FAILED",
                    null, null, "WALLET.WITHDRAWAL.WORKFLOW_FAILED", ex.getMessage());
        }
    }

    private WithdrawalResult compensate(WithdrawalInput input, String code, String message) {
        status = "COMPENSATING";
        try {
            activities.refundFineract(input.fineractSavingsId(), input.totalDebit(),
                    "REFUND:" + input.withdrawalNumber());
            status = "COMPENSATED";
            activities.markStatus(input.withdrawalId(), "COMPENSATED",
                    bankReference, sarieReference, code, message);
            return new WithdrawalResult(input.withdrawalId(), "COMPENSATED",
                    bankReference, sarieReference, code, message);
        } catch (Exception ex) {
            log.error("CRITICAL: refund failed withdrawalId={}: {}",
                    input.withdrawalId(), ex.getMessage());
            status = "COMPENSATION_FAILED";
            activities.markStatus(input.withdrawalId(), "FAILED",
                    bankReference, sarieReference,
                    "WALLET.WITHDRAWAL.COMPENSATION_FAILED",
                    "Refund failed: " + ex.getMessage());
            return new WithdrawalResult(input.withdrawalId(), "FAILED",
                    bankReference, sarieReference,
                    "WALLET.WITHDRAWAL.COMPENSATION_FAILED",
                    "Refund failed: " + ex.getMessage());
        }
    }

    @Override
    public void onBankSettlementConfirmed(String bankReference, String sarieReference) {
        log.info("Settlement signal: ACCEPTED bankRef={}", bankReference);
        this.bankReference = bankReference;
        this.sarieReference = sarieReference;
        this.settlementAccepted = true;
        this.settlementSignalled = true;
    }

    @Override
    public void onBankSettlementRejected(String reasonCode, String reasonMessage) {
        log.info("Settlement signal: REJECTED code={}", reasonCode);
        this.rejectionCode = reasonCode;
        this.rejectionMessage = reasonMessage;
        this.settlementAccepted = false;
        this.settlementSignalled = true;
    }

    @Override
    public String getStatus() {
        return status;
    }
}
