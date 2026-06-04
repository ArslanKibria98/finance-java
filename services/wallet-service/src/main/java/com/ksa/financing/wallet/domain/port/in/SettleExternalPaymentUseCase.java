package com.ksa.financing.wallet.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Settle a payment that has already moved on the Scotia rail: mirror the money into our
 * wallets by debtor/creditor account number. Whichever side belongs to one of our wallets
 * is debited (debtor) / credited (creditor); a purely external side is skipped.
 *
 * Triggered by middleware-third-party after a successful SCOTIABANK_PAYMENT_COMMIT.
 */
public interface SettleExternalPaymentUseCase {

    SettleResult settle(SettleCommand command);

    /**
     * Pre-flight check (no mutation): debtorAccount must be present, one of our wallets, active,
     * Fineract-linked, and hold sufficient available balance. Throws otherwise. Call this BEFORE
     * the payment rail so insufficient/invalid is surfaced without moving any money.
     */
    void validate(SettleCommand command);

    record SettleCommand(
            UUID tenantId,
            String debtorAccount,
            String debtorMobile,      // when debtorAccount is absent, resolve debtor wallet by this mobile (from JWT)
            String creditorAccount,
            BigDecimal amount,
            String currency,
            String reference,
            String idempotencyKey
    ) {}

    record SettleResult(
            boolean debtorDebited,
            UUID debtorWalletId,
            UUID debtorMovementId,
            boolean creditorCredited,
            UUID creditorWalletId,
            UUID creditorMovementId,
            BigDecimal amount,
            String currency
    ) {}
}
