package com.ksa.financing.wallet.domain.iso;

/**
 * ISO 20022 ChargeBearerType1Code — who pays the transaction fees.
 * <p>
 *   DEBT (default) → Debtor (sender) pays all charges
 *   CRED           → Creditor (beneficiary) pays all charges
 *   SHAR           → Shared — sender pays own bank, beneficiary pays receiver bank
 *   SLEV           → Service-level agreed (charges per pre-agreed contract)
 */
public enum ChargeBearerType1Code {
    DEBT,
    CRED,
    SHAR,
    SLEV;

    public static ChargeBearerType1Code parseOrDefault(String input) {
        if (input == null || input.isBlank()) return DEBT;
        try {
            return valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DEBT;
        }
    }
}
