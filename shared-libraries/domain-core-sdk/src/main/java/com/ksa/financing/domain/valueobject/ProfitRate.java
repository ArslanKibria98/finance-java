package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing an annual profit rate for Islamic financing.
 * <p>
 * Profit rates are stored as decimal values (e.g., 0.0525 for 5.25%) with 4 decimal precision.
 * Valid range: 0.01% to 50% (0.0001 to 0.5000 in decimal form).
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record ProfitRate(
        @NotNull
        @DecimalMin(value = "0.0001", message = "Profit rate must be at least 0.01%")
        @DecimalMax(value = "0.5000", message = "Profit rate cannot exceed 50%")
        BigDecimal value
) {
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal MIN_RATE = new BigDecimal("0.0001");
    private static final BigDecimal MAX_RATE = new BigDecimal("0.5000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Canonical constructor with validation.
     */
    public ProfitRate {
        Objects.requireNonNull(value, "Profit rate value cannot be null");

        // Ensure 4 decimal precision
        value = value.setScale(SCALE, ROUNDING_MODE);

        // Validate range
        if (value.compareTo(MIN_RATE) < 0) {
            throw new IllegalArgumentException(
                    String.format("Profit rate must be at least 0.01%%. Got: %s", value)
            );
        }
        if (value.compareTo(MAX_RATE) > 0) {
            throw new IllegalArgumentException(
                    String.format("Profit rate cannot exceed 50%%. Got: %s", value)
            );
        }
    }

    /**
     * Create a ProfitRate from a decimal value (e.g., 0.0525 for 5.25%).
     *
     * @param decimalValue the decimal representation of the rate
     * @return a new ProfitRate instance
     */
    public static ProfitRate ofDecimal(BigDecimal decimalValue) {
        return new ProfitRate(decimalValue);
    }

    /**
     * Create a ProfitRate from a decimal value (e.g., 0.0525 for 5.25%).
     *
     * @param decimalValue the decimal representation of the rate
     * @return a new ProfitRate instance
     */
    public static ProfitRate ofDecimal(double decimalValue) {
        return new ProfitRate(BigDecimal.valueOf(decimalValue));
    }

    /**
     * Create a ProfitRate from a percentage value (e.g., 5.25 for 5.25%).
     *
     * @param percentageValue the percentage representation of the rate
     * @return a new ProfitRate instance
     */
    public static ProfitRate ofPercentage(BigDecimal percentageValue) {
        Objects.requireNonNull(percentageValue, "Percentage value cannot be null");
        return new ProfitRate(percentageValue.divide(HUNDRED, SCALE, ROUNDING_MODE));
    }

    /**
     * Create a ProfitRate from a percentage value (e.g., 5.25 for 5.25%).
     *
     * @param percentageValue the percentage representation of the rate
     * @return a new ProfitRate instance
     */
    public static ProfitRate ofPercentage(double percentageValue) {
        return ofPercentage(BigDecimal.valueOf(percentageValue));
    }

    /**
     * Get the rate as a percentage value (e.g., 5.25 for 5.25%).
     *
     * @return the percentage representation
     */
    public BigDecimal asPercentage() {
        return value.multiply(HUNDRED).setScale(2, ROUNDING_MODE);
    }

    /**
     * Get the rate as a decimal value (e.g., 0.0525 for 5.25%).
     *
     * @return the decimal representation
     */
    public BigDecimal asDecimal() {
        return value;
    }

    /**
     * Calculate the profit amount by multiplying the rate with a principal amount.
     *
     * @param principal the principal amount
     * @return the calculated profit amount
     */
    public SarMoney multiply(SarMoney principal) {
        Objects.requireNonNull(principal, "Principal amount cannot be null");
        return principal.multiply(value);
    }

    /**
     * Add another profit rate to this one.
     *
     * @param other the other profit rate
     * @return a new ProfitRate with the sum
     */
    public ProfitRate add(ProfitRate other) {
        Objects.requireNonNull(other, "Other profit rate cannot be null");
        return new ProfitRate(this.value.add(other.value));
    }

    /**
     * Subtract another profit rate from this one.
     *
     * @param other the other profit rate
     * @return a new ProfitRate with the difference
     */
    public ProfitRate subtract(ProfitRate other) {
        Objects.requireNonNull(other, "Other profit rate cannot be null");
        BigDecimal result = this.value.subtract(other.value);
        if (result.compareTo(MIN_RATE) < 0) {
            throw new IllegalArgumentException("Resulting profit rate would be below minimum");
        }
        return new ProfitRate(result);
    }

    /**
     * Format as a percentage string (e.g., "5.25%").
     *
     * @return formatted percentage string
     */
    public String toPercentageString() {
        return String.format("%,.2f%%", asPercentage());
    }

    @Override
    public String toString() {
        return toPercentageString();
    }
}
