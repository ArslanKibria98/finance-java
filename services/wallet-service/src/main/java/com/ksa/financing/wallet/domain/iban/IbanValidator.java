package com.ksa.financing.wallet.domain.iban;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * IBAN validator implementing ISO 13616:
 *   1. Format check (uppercase + alphanumeric, 5-34 chars)
 *   2. Country length check (per registry)
 *   3. MOD-97 checksum check
 * <p>
 * Pure Java — no framework imports. Safe for domain layer.
 */
public final class IbanValidator {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$");
    private static final BigInteger MOD = BigInteger.valueOf(97);
    private static final BigInteger ONE = BigInteger.ONE;

    private IbanValidator() {}

    /**
     * Validate without throwing — returns rich result.
     */
    public static ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            return new ValidationResult(false, "IBAN.REQUIRED", "IBAN is required",
                    null, null);
        }
        String iban = normalize(input);

        if (!FORMAT.matcher(iban).matches()) {
            return new ValidationResult(false, "IBAN.FORMAT",
                    "IBAN format is invalid (must start with 2-letter country + 2 digits)",
                    iban, null);
        }

        String country = iban.substring(0, 2);
        var expectedLen = IbanCountrySpec.expectedLength(country);
        if (expectedLen.isEmpty()) {
            return new ValidationResult(false, "IBAN.COUNTRY_UNSUPPORTED",
                    "IBAN country code not supported: " + country,
                    iban, country);
        }
        if (iban.length() != expectedLen.get()) {
            return new ValidationResult(false, "IBAN.LENGTH",
                    "IBAN length invalid for " + country
                            + " (expected " + expectedLen.get() + ", got " + iban.length() + ")",
                    iban, country);
        }

        if (!checksumValid(iban)) {
            return new ValidationResult(false, "IBAN.CHECKSUM",
                    "IBAN checksum failed (MOD-97 mismatch)",
                    iban, country);
        }

        return new ValidationResult(true, null, null, iban, country);
    }

    /**
     * Strip whitespace and convert to uppercase. Does NOT validate.
     */
    public static String normalize(String input) {
        if (input == null) return null;
        return input.replaceAll("\\s+", "").toUpperCase();
    }

    /**
     * Quick boolean — wraps {@link #validate}.
     */
    public static boolean isValid(String input) {
        return validate(input).valid();
    }

    /**
     * Mask IBAN for safe display: first 4 + last 4, rest masked.
     * Example: "SA44...1234"
     */
    public static String maskForDisplay(String iban) {
        if (iban == null) return null;
        String norm = normalize(iban);
        if (norm.length() < 8) return "****";
        return norm.substring(0, 4) + "..." + norm.substring(norm.length() - 4);
    }

    /**
     * Country code (first 2 chars), or null if not present.
     */
    public static String country(String iban) {
        if (iban == null) return null;
        String norm = normalize(iban);
        return norm.length() >= 2 ? norm.substring(0, 2) : null;
    }

    /**
     * MOD-97 checksum per ISO 13616:
     *   1. Move first 4 chars to end
     *   2. Replace letters with numbers (A=10, B=11, ..., Z=35)
     *   3. Remainder when divided by 97 must be 1
     */
    private static boolean checksumValid(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder(rearranged.length() * 2);
        for (int i = 0; i < rearranged.length(); i++) {
            char c = rearranged.charAt(i);
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                numeric.append(c - 'A' + 10);
            } else {
                return false;
            }
        }
        try {
            return new BigInteger(numeric.toString()).mod(MOD).equals(ONE);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Validation outcome — never throws, fully describes the result.
     */
    public record ValidationResult(
            boolean valid,
            String  errorCode,
            String  errorMessage,
            String  normalizedIban,
            String  countryCode
    ) {
        public ValidationResult requireValid() {
            if (!valid) {
                throw new IllegalArgumentException(
                        (errorCode != null ? errorCode + ": " : "") + errorMessage);
            }
            return this;
        }
    }
}
