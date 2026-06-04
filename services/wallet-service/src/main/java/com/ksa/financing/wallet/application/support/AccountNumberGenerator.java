package com.ksa.financing.wallet.application.support;

/**
 * Mints virtual Canadian-format account numbers for wallets.
 *
 * Format: {@code <fi>-<transit>-<7-digit sequence>}  e.g. {@code 002-80150-0000123}
 *   - fi       : financial-institution number (Scotia = 002)
 *   - transit  : 5-digit branch transit
 *   - sequence : zero-padded running number minted from a shared DB sequence
 *
 * This value is used as the RTP debtor/creditor {@code identification} reference.
 * Pure formatter — no framework dependencies.
 */
public final class AccountNumberGenerator {

    private AccountNumberGenerator() {
    }

    public static String generate(String fiNumber, String transit, long sequence) {
        return fiNumber + "-" + transit + "-" + String.format("%07d", sequence);
    }
}
