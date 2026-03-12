package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Handles loan disbursement via Fineract core banking and bank transfers.
 * Used in the final phase of the loan application workflow.
 */
@ActivityInterface
public interface DisbursementActivity {

    @ActivityMethod
    FineractResult registerWithFineract(FineractInput input);

    @ActivityMethod
    DisbursementResult disburseFunds(DisburseFundsInput input);

    @ActivityMethod
    void sendCompletionNotification(NotificationInput input);

    // ══════════ DTOs ══════════

    record FineractInput(
            String tenantId,
            String loanId,
            String customerId,
            String productCode,
            BigDecimal principalAmount,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal installmentAmount,
            String shariaStructure
    ) {}

    record FineractResult(
            long fineractLoanId,
            String fineractClientId,
            boolean registered
    ) {}

    record DisburseFundsInput(
            String tenantId,
            String loanId,
            String loanNumber,
            BigDecimal amount,
            String iban,
            String bankCode,
            String beneficiaryName,
            String idempotencyKey
    ) {}

    record DisbursementResult(
            String disbursementId,
            String disbursementNumber,
            String paymentReference,
            String status,
            boolean success
    ) {}

    record NotificationInput(
            String tenantId,
            String customerId,
            String mobileNumber,
            String applicationNumber,
            String loanNumber,
            BigDecimal disbursedAmount,
            String notificationType      // SMS, PUSH, BOTH
    ) {}
}
