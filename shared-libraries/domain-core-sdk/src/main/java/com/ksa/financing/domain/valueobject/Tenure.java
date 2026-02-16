package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Immutable value object representing a loan tenure (duration) in months.
 * <p>
 * Tenure represents the length of a financing contract, typically expressed in months.
 * Valid range: 1 to 360 months (1 month to 30 years).
 * </p>
 * <p>
 * This value object provides helper methods for calculating tenure-related values
 * such as the total number of days for a given start date.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record Tenure(
        @Min(value = 1, message = "Tenure must be at least 1 month")
        @Max(value = 360, message = "Tenure cannot exceed 360 months")
        int months
) {
    private static final int MIN_MONTHS = 1;
    private static final int MAX_MONTHS = 360;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int AVERAGE_DAYS_PER_MONTH = 30;

    /**
     * Canonical constructor with validation.
     */
    public Tenure {
        if (months < MIN_MONTHS) {
            throw new IllegalArgumentException(
                    String.format("Tenure must be at least %d month. Got: %d", MIN_MONTHS, months)
            );
        }
        if (months > MAX_MONTHS) {
            throw new IllegalArgumentException(
                    String.format("Tenure cannot exceed %d months. Got: %d", MAX_MONTHS, months)
            );
        }
    }

    /**
     * Create a Tenure from a number of months.
     *
     * @param months the number of months
     * @return a new Tenure instance
     */
    public static Tenure ofMonths(int months) {
        return new Tenure(months);
    }

    /**
     * Create a Tenure from a number of years.
     *
     * @param years the number of years
     * @return a new Tenure instance
     */
    public static Tenure ofYears(int years) {
        if (years < 1) {
            throw new IllegalArgumentException("Years must be at least 1");
        }
        return new Tenure(years * MONTHS_PER_YEAR);
    }

    /**
     * Create a Tenure from years and months.
     *
     * @param years       the number of years
     * @param extraMonths the additional months
     * @return a new Tenure instance
     */
    public static Tenure of(int years, int extraMonths) {
        if (years < 0) {
            throw new IllegalArgumentException("Years cannot be negative");
        }
        if (extraMonths < 0 || extraMonths >= MONTHS_PER_YEAR) {
            throw new IllegalArgumentException("Extra months must be between 0 and 11");
        }
        return new Tenure(years * MONTHS_PER_YEAR + extraMonths);
    }

    /**
     * Get the number of complete years in this tenure.
     *
     * @return the number of years
     */
    public int getYears() {
        return months / MONTHS_PER_YEAR;
    }

    /**
     * Get the remaining months after complete years.
     *
     * @return the remaining months (0-11)
     */
    public int getRemainingMonths() {
        return months % MONTHS_PER_YEAR;
    }

    /**
     * Calculate the total number of days for this tenure starting from a given date.
     * <p>
     * This method accurately calculates the number of days by adding the months
     * to the start date and computing the difference, accounting for varying
     * month lengths and leap years.
     * </p>
     *
     * @param startDate the start date
     * @return the total number of days
     */
    public long getTotalDays(LocalDate startDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        LocalDate endDate = startDate.plusMonths(months);
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Calculate the approximate number of days using 30-day months.
     * <p>
     * This is a simplified calculation that may be used for estimates.
     * For accurate calculations, use {@link #getTotalDays(LocalDate)}.
     * </p>
     *
     * @return the approximate number of days
     */
    public int getApproximateDays() {
        return months * AVERAGE_DAYS_PER_MONTH;
    }

    /**
     * Calculate the end date given a start date.
     *
     * @param startDate the start date
     * @return the end date after adding this tenure
     */
    public LocalDate getEndDate(LocalDate startDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        return startDate.plusMonths(months);
    }

    /**
     * Add months to this tenure.
     *
     * @param additionalMonths the number of months to add
     * @return a new Tenure instance
     */
    public Tenure plusMonths(int additionalMonths) {
        return new Tenure(this.months + additionalMonths);
    }

    /**
     * Add years to this tenure.
     *
     * @param additionalYears the number of years to add
     * @return a new Tenure instance
     */
    public Tenure plusYears(int additionalYears) {
        return new Tenure(this.months + (additionalYears * MONTHS_PER_YEAR));
    }

    /**
     * Subtract months from this tenure.
     *
     * @param monthsToSubtract the number of months to subtract
     * @return a new Tenure instance
     */
    public Tenure minusMonths(int monthsToSubtract) {
        int newMonths = this.months - monthsToSubtract;
        if (newMonths < MIN_MONTHS) {
            throw new IllegalArgumentException("Resulting tenure would be less than minimum");
        }
        return new Tenure(newMonths);
    }

    /**
     * Check if this tenure is longer than another tenure.
     *
     * @param other the other tenure
     * @return true if this tenure is longer
     */
    public boolean isLongerThan(Tenure other) {
        Objects.requireNonNull(other, "Other tenure cannot be null");
        return this.months > other.months;
    }

    /**
     * Check if this tenure is shorter than another tenure.
     *
     * @param other the other tenure
     * @return true if this tenure is shorter
     */
    public boolean isShorterThan(Tenure other) {
        Objects.requireNonNull(other, "Other tenure cannot be null");
        return this.months < other.months;
    }

    /**
     * Format as a human-readable string.
     * Examples: "24 months", "1 year", "2 years 6 months"
     *
     * @return formatted string
     */
    public String toFormattedString() {
        int years = getYears();
        int remainingMonths = getRemainingMonths();

        if (years == 0) {
            return months + (months == 1 ? " month" : " months");
        } else if (remainingMonths == 0) {
            return years + (years == 1 ? " year" : " years");
        } else {
            return String.format("%d %s %d %s",
                    years, years == 1 ? "year" : "years",
                    remainingMonths, remainingMonths == 1 ? "month" : "months");
        }
    }

    @Override
    public String toString() {
        return toFormattedString();
    }
}
