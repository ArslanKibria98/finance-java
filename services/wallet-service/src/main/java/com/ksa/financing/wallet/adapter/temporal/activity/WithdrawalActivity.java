package com.ksa.financing.wallet.adapter.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Temporal activities for the withdrawal SAGA.
 * Implementations live in {@code com.ksa.financing.wallet.adapter.temporal.activity.impl}.
 */
@ActivityInterface
public interface WithdrawalActivity {

    @ActivityMethod
    String debitFineract(Long savingsId, BigDecimal totalDebit, String externalRef);

    @ActivityMethod
    String refundFineract(Long savingsId, BigDecimal totalDebit, String externalRef);

    @ActivityMethod
    BankRailsResult submitToBankRails(BankRailsInput input);

    @ActivityMethod
    void markStatus(UUID withdrawalId, String status, String bankRef, String sarieRef,
                    String errorCode, String errorMessage);

    record BankRailsInput(
            String withdrawalNumber,
            String idempotencyKey,
            String beneficiaryName,
            String destinationIban,
            String destinationBankCode,
            BigDecimal amount,
            String currency,
            String purposeCode,
            String narrative
    ) {}

    record BankRailsResult(
            boolean accepted,
            String bankReference,
            String sarieReference,
            String rejectionCode,
            String rejectionMessage
    ) {}
}
