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

    /**
     * Credit loan proceeds directly into the customer's wallet (Fineract savings account)
     * instead of bank transfer to IBAN. Wallet-service handles the Fineract deposit.
     */
    @ActivityMethod
    WalletDisbursementResult disburseToWallet(WalletDisbursementInput input);

    @ActivityMethod
    void sendCompletionNotification(NotificationInput input);

    // ══════════ ANB B2B DISBURSEMENT (BRD V1.8 Step 64-69) ══════════

    @ActivityMethod
    AnbTransferResult transferViaAnb(AnbTransferInput input);

    // ══════════ DTOs ══════════

    record FineractInput(
            String tenantId,
            String loanId,
            String customerId,
            String productCode,
            String fineractProductId,
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

    record WalletDisbursementInput(
            String tenantId,
            String loanId,
            String loanNumber,
            String customerId,
            BigDecimal amount,
            String idempotencyKey
    ) {}

    record WalletDisbursementResult(
            String walletId,
            String walletNumber,
            Long fineractSavingsAccountId,
            Long fineractTransactionId,
            String movementId,
            BigDecimal newAvailableBalance,
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

    // ══════════ ANB B2B DTOs ══════════

    record AnbTransferInput(
            String tenantId,
            String loanId,
            String loanNumber,
            BigDecimal amount,
            String iban,
            String bankCode,
            String beneficiaryName,
            String idempotencyKey
    ) {}

    record AnbTransferResult(
            String transactionId,
            String status,
            boolean success,
            String errorMessage
    ) {}
}
