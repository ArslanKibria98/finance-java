package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.TransactionPurpose;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Debits a wallet from an internal source (loan repayment, fee deduction, etc.).
 * Funds are withdrawn from the Fineract savings account backing the wallet —
 * Fineract remains the source of truth for balance.
 *
 * Mirrors {@link CreditWalletUseCase}.
 */
public interface DebitWalletUseCase {

    DebitResult debit(DebitCommand command);

    record DebitCommand(
            UUID tenantId,
            UUID customerId,
            BigDecimal amount,
            TransactionPurpose purpose,
            String referenceType,   // e.g. "LOAN_REPAYMENT"
            UUID referenceId,        // e.g. loan UUID
            String description,
            String idempotencyKey
    ) {}

    record DebitResult(
            UUID walletId,
            String walletNumber,
            Long fineractSavingsAccountId,
            Long fineractTransactionId,
            BigDecimal amount,
            BigDecimal newAvailableBalance,
            UUID movementId,
            String status
    ) {}
}
