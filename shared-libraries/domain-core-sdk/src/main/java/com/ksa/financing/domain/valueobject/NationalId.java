package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Immutable value object representing a Saudi National ID (Iqama number for residents).
 * <p>
 * Saudi National ID format:
 * - Total 10 digits
 * - First digit indicates type: 1 = Saudi Citizen, 2 = Resident (Iqama)
 * - Remaining 9 digits are the unique identifier
 * </p>
 * <p>
 * Example:
 * - 1234567890 (Saudi citizen)
 * - 2987654321 (Resident)
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record NationalId(
        @NotBlank(message = "National ID cannot be blank")
        @Pattern(regexp = "^[12]\\d{9}$", message = "National ID must be 10 digits starting with 1 or 2")
        String value
) {
    private static final int TOTAL_LENGTH = 10;
    private static final char CITIZEN_TYPE = '1';
    private static final char RESIDENT_TYPE = '2';

    /**
     * Canonical constructor with validation.
     */
    public NationalId {
        Objects.requireNonNull(value, "National ID cannot be null");

        // Remove any spaces or dashes
        value = value.replaceAll("[\\s-]", "");

        // Validate format
        if (!value.matches("^[12]\\d{9}$")) {
            throw new IllegalArgumentException(
                    String.format("Invalid National ID format. Must be 10 digits starting with 1 or 2, got: %s", value)
            );
        }
    }

    /**
     * Create a NationalId from a string value.
     *
     * @param idString the national ID string
     * @return a new NationalId instance
     */
    public static NationalId of(String idString) {
        return new NationalId(idString);
    }

    /**
     * Create a NationalId for a Saudi citizen.
     *
     * @param uniqueId the 9-digit unique identifier
     * @return a new NationalId instance starting with 1
     */
    public static NationalId ofCitizen(String uniqueId) {
        Objects.requireNonNull(uniqueId, "Unique ID cannot be null");
        if (!uniqueId.matches("^\\d{9}$")) {
            throw new IllegalArgumentException("Unique ID must be exactly 9 digits");
        }
        return new NationalId(CITIZEN_TYPE + uniqueId);
    }

    /**
     * Create a NationalId for a resident (Iqama).
     *
     * @param uniqueId the 9-digit unique identifier
     * @return a new NationalId instance starting with 2
     */
    public static NationalId ofResident(String uniqueId) {
        Objects.requireNonNull(uniqueId, "Unique ID cannot be null");
        if (!uniqueId.matches("^\\d{9}$")) {
            throw new IllegalArgumentException("Unique ID must be exactly 9 digits");
        }
        return new NationalId(RESIDENT_TYPE + uniqueId);
    }

    /**
     * Get the type indicator (first digit).
     *
     * @return the type indicator character
     */
    public char getTypeIndicator() {
        return value.charAt(0);
    }

    /**
     * Check if this ID belongs to a Saudi citizen.
     *
     * @return true if citizen, false otherwise
     */
    public boolean isCitizen() {
        return value.charAt(0) == CITIZEN_TYPE;
    }

    /**
     * Check if this ID belongs to a resident (Iqama holder).
     *
     * @return true if resident, false otherwise
     */
    public boolean isResident() {
        return value.charAt(0) == RESIDENT_TYPE;
    }

    /**
     * Get the unique identifier (last 9 digits).
     *
     * @return the unique identifier
     */
    public String getUniqueIdentifier() {
        return value.substring(1);
    }

    /**
     * Get the type as a human-readable string.
     *
     * @return "Citizen" or "Resident"
     */
    public String getTypeDescription() {
        return isCitizen() ? "Citizen" : "Resident";
    }

    /**
     * Mask the ID for display purposes, showing only the first and last 2 digits.
     * Example: 12******90
     *
     * @return masked national ID
     */
    public String toMaskedString() {
        return value.substring(0, 2) + "******" + value.substring(8);
    }

    /**
     * Format the ID with dashes for readability.
     * Example: 1-234-567-890
     *
     * @return formatted national ID with dashes
     */
    public String toFormattedString() {
        return String.format("%s-%s-%s-%s",
                value.substring(0, 1),
                value.substring(1, 4),
                value.substring(4, 7),
                value.substring(7, 10)
        );
    }

    @Override
    public String toString() {
        return value;
    }
}
