package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.TransactionPurpose;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Credits a wallet from an internal source (loan disbursement, refund, etc.).
 * Funds are pushed to the Fineract savings account backing the wallet —
 * Fineract remains the source of truth for balance.
 */
public interface CreditWalletUseCase {

    CreditResult credit(CreditCommand command);

    record CreditCommand(
            UUID tenantId,
            UUID customerId,
            BigDecimal amount,
            TransactionPurpose purpose,
            String referenceType,   // e.g. "LOAN_DISBURSEMENT"
            UUID referenceId,        // e.g. loan UUID
            String description,
            String idempotencyKey
    ) {}

    record CreditResult(
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
