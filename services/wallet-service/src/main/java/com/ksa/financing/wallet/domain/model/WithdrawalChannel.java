package com.ksa.financing.wallet.domain.model;

/**
 * Settlement channel for an outbound withdrawal.
 *
 * BANK_IBAN          → standard SAMA bank transfer (T+0 / T+1)
 * INSTANT_SARIE      → SAMA SARIE instant payment rails
 * INTERNAL_TRANSFER  → cross-bank internal (same FI)
 * OWN_BANK_ACCOUNT   → wallet → linked own bank account (no third-party)
 */
public enum WithdrawalChannel {
    BANK_IBAN,
    INSTANT_SARIE,
    INTERNAL_TRANSFER,
    OWN_BANK_ACCOUNT
}
