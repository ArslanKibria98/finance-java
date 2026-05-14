package com.ksa.financing.wallet.domain.iban;

import java.math.BigInteger;

/**
 * Deterministic IBAN generator for Saudi Arabia (SA) IBANs derived from a
 * Fineract savings account identifier.
 * <p>
 * Format (ISO 13616, length 24):
 * <pre>
 *   SA | check-digits(2) | bank-code(2) | account-number(18)
 * </pre>
 * The 18-digit account number is the zero-padded Fineract savings account id,
 * guaranteeing a 1:1 mapping between a Fineract account and the IBAN exposed in
 * profile / wallet APIs. Check digits are computed via MOD-97 so the result
 * passes {@link IbanValidator}.
 * <p>
 * Pure Java — no framework imports. Safe for domain layer.
 */
public final class IbanGenerator {

    private static final String COUNTRY = "SA";
    private static final int ACCOUNT_NUMBER_LENGTH = 18;
    private static final BigInteger MOD = BigInteger.valueOf(97);
    private static final BigInteger NINETY_EIGHT = BigInteger.valueOf(98);

    private IbanGenerator() {}

    /**
     * Generate a deterministic, MOD-97-valid SA IBAN for a Fineract savings account.
     *
     * @param fineractSavingsAccountId Fineract savings account id (positive)
     * @param bankCode                 2-digit bank code (e.g. "80"); must be exactly 2 digits
     * @return valid SA IBAN, 24 chars
     */
    public static String fromFineractSavingsId(long fineractSavingsAccountId, String bankCode) {
        if (fineractSavingsAccountId <= 0) {
            throw new IllegalArgumentException(
                    "fineractSavingsAccountId must be positive, got: " + fineractSavingsAccountId);
        }
        if (bankCode == null || !bankCode.matches("\\d{2}")) {
            throw new IllegalArgumentException(
                    "bankCode must be exactly 2 digits, got: " + bankCode);
        }
        String accountNumber = padLeft(Long.toString(fineractSavingsAccountId), ACCOUNT_NUMBER_LENGTH);
        if (accountNumber.length() != ACCOUNT_NUMBER_LENGTH) {
            throw new IllegalArgumentException(
                    "Fineract savings id exceeds " + ACCOUNT_NUMBER_LENGTH + " digits: " + fineractSavingsAccountId);
        }
        String checkDigits = computeCheckDigits(bankCode, accountNumber);
        return COUNTRY + checkDigits + bankCode + accountNumber;
    }

    /**
     * MOD-97 check digit computation (ISO 13616):
     *   1. Build BBAN = bankCode + accountNumber
     *   2. Append country (SA = "2810") and placeholder check digits "00"
     *   3. checkDigits = 98 - (numeric mod 97)
     */
    private static String computeCheckDigits(String bankCode, String accountNumber) {
        // S = 28, A = 10  → SA = "2810"
        String numeric = bankCode + accountNumber + "2810" + "00";
        int remainder = new BigInteger(numeric).mod(MOD).intValueExact();
        int check = NINETY_EIGHT.intValueExact() - remainder;
        return String.format("%02d", check);
    }

    private static String padLeft(String value, int length) {
        if (value.length() >= length) return value;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length - value.length(); i++) sb.append('0');
        sb.append(value);
        return sb.toString();
    }
}
