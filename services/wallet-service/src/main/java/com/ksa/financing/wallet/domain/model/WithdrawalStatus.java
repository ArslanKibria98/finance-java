package com.ksa.financing.wallet.domain.model;

/**
 * State machine for outbound wallet withdrawals (cash-out to bank IBAN).
 * <p>
 * PENDING        → request received, not yet validated
 * VALIDATED      → limits/balance checked
 * DEBITED        → funds debited from Fineract savings (point-of-no-return for SAGA)
 * BANK_SUBMITTED → handed off to bank rails (SARIE / mock)
 * COMPLETED      → bank settled — terminal success
 * FAILED         → failed before debit — no compensation needed
 * COMPENSATED    → bank rails rejected after debit; funds refunded — terminal
 * CANCELLED      → user/admin cancelled before submission
 */
public enum WithdrawalStatus {
    PENDING,
    VALIDATED,
    HELD_AML,
    DEBITED,
    BANK_SUBMITTED,
    COMPLETED,
    FAILED,
    COMPENSATED,
    CANCELLED
}
