package com.ksa.financing.wallet.adapter.temporal.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Temporal workflow for asynchronous withdrawal settlement.
 * <p>
 * Used when the bank rails settle out-of-band (T+0/T+1) and we need
 * durable state machine + retry + compensation across hours/days.
 * <p>
 * For sync demo flow we still use {@code InitiateWithdrawalService} directly;
 * this workflow is the production path for real SARIE integrations.
 * <p>
 * Saga steps:
 *   1. validateAndPersist
 *   2. debitFineract                  ↦ compensation: refundFineract
 *   3. submitToBankRails               ↦ compensation: cancelBankSubmission (best-effort)
 *   4. waitForSettlement (signal)      ↦ on timeout: refundFineract
 *   5. markCompleted
 */
@WorkflowInterface
public interface WithdrawalWorkflow {

    @WorkflowMethod
    WithdrawalResult execute(WithdrawalInput input);

    /** Signal sent by bank rails callback when settlement is confirmed. */
    @io.temporal.workflow.SignalMethod
    void onBankSettlementConfirmed(String bankReference, String sarieReference);

    /** Signal sent by bank rails callback when settlement is rejected after submission. */
    @io.temporal.workflow.SignalMethod
    void onBankSettlementRejected(String reasonCode, String reasonMessage);

    @QueryMethod
    String getStatus();

    record WithdrawalInput(
            UUID withdrawalId,
            UUID tenantId,
            UUID sourceWalletId,
            Long fineractSavingsId,
            String withdrawalNumber,
            String destinationIban,
            String beneficiaryName,
            BigDecimal totalDebit,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {}

    record WithdrawalResult(
            UUID withdrawalId,
            String finalStatus,
            String bankReference,
            String sarieReference,
            String errorCode,
            String errorMessage
    ) {}
}
