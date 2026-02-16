package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Immutable value object representing a Saudi IBAN (International Bank Account Number).
 * <p>
 * Saudi IBAN format: SA + 2 check digits + 20 digits (total 24 characters).
 * Example: SA0380000000608010167519
 * </p>
 * <p>
 * This implementation validates the IBAN using the MOD-97 checksum algorithm
 * as defined in ISO 13616.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record IBAN(
        @NotBlank(message = "IBAN cannot be blank")
        @Pattern(regexp = "^SA\\d{22}$", message = "IBAN must follow Saudi format: SA + 22 digits")
        String value
) {
    private static final int TOTAL_LENGTH = 24;
    private static final String COUNTRY_CODE = "SA";
    private static final int CHECKSUM_MODULUS = 97;
    private static final int VALID_CHECKSUM = 1;

    /**
     * Canonical constructor with validation.
     */
    public IBAN {
        Objects.requireNonNull(value, "IBAN cannot be null");

        // Normalize: remove spaces and convert to uppercase
        value = value.replaceAll("\\s", "").toUpperCase();

        // Validate format
        if (!value.matches("^SA\\d{22}$")) {
            throw new IllegalArgumentException(
                    String.format("Invalid Saudi IBAN format. Expected SA + 22 digits, got: %s", value)
            );
        }

        // Validate checksum using MOD-97 algorithm
        if (!isValidChecksum(value)) {
            throw new IllegalArgumentException(
                    String.format("Invalid IBAN checksum: %s", value)
            );
        }
    }

    /**
     * Create an IBAN from a string value.
     *
     * @param ibanString the IBAN string
     * @return a new IBAN instance
     */
    public static IBAN of(String ibanString) {
        return new IBAN(ibanString);
    }

    /**
     * Validate the IBAN checksum using the MOD-97 algorithm.
     * <p>
     * Algorithm:
     * 1. Move the first 4 characters to the end
     * 2. Replace letters with numbers (A=10, B=11, ..., Z=35)
     * 3. Calculate mod 97 of the resulting number
     * 4. Valid if result equals 1
     * </p>
     *
     * @param iban the IBAN string to validate
     * @return true if checksum is valid, false otherwise
     */
    private static boolean isValidChecksum(String iban) {
        // Move first 4 characters to the end
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Replace letters with numbers (A=10, B=11, ..., Z=35)
        StringBuilder numericString = new StringBuilder();
        for (char ch : rearranged.toCharArray()) {
            if (Character.isDigit(ch)) {
                numericString.append(ch);
            } else {
                // Convert letter to number (A=10, B=11, etc.)
                numericString.append(Character.getNumericValue(ch));
            }
        }

        // Calculate mod 97
        BigInteger ibanNumber = new BigInteger(numericString.toString());
        int remainder = ibanNumber.mod(BigInteger.valueOf(CHECKSUM_MODULUS)).intValue();

        return remainder == VALID_CHECKSUM;
    }

    /**
     * Get the country code (always "SA" for Saudi IBAN).
     *
     * @return the country code
     */
    public String getCountryCode() {
        return COUNTRY_CODE;
    }

    /**
     * Get the check digits (positions 3-4).
     *
     * @return the check digits as a string
     */
    public String getCheckDigits() {
        return value.substring(2, 4);
    }

    /**
     * Get the basic bank account number (BBAN) - the 20 digits after the check digits.
     *
     * @return the BBAN
     */
    public String getBban() {
        return value.substring(4);
    }

    /**
     * Get the bank code (first 2 digits of BBAN).
     * <p>
     * In Saudi IBANs, the first 2 digits of the BBAN represent the bank code.
     * </p>
     *
     * @return the bank code
     */
    public String getBankCode() {
        return value.substring(4, 6);
    }

    /**
     * Get the account number (remaining digits after bank code).
     *
     * @return the account number
     */
    public String getAccountNumber() {
        return value.substring(6);
    }

    /**
     * Format the IBAN with spaces for readability (groups of 4).
     * Example: SA03 8000 0000 6080 1016 7519
     *
     * @return formatted IBAN with spaces
     */
    public String toFormattedString() {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < value.length(); i += 4) {
            if (i > 0) {
                formatted.append(' ');
            }
            formatted.append(value.substring(i, Math.min(i + 4, value.length())));
        }
        return formatted.toString();
    }

    /**
     * Mask the IBAN for display purposes, showing only the last 4 digits.
     * Example: SA** **** **** **** **** 7519
     *
     * @return masked IBAN
     */
    public String toMaskedString() {
        return "SA** **** **** **** **** " + value.substring(20);
    }

    @Override
    public String toString() {
        return value;
    }
}
