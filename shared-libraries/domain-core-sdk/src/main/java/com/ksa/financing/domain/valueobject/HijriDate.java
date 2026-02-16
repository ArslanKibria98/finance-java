package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Immutable value object representing a Hijri (Islamic) calendar date.
 * <p>
 * This wrapper provides a convenient API for working with Hijri dates using
 * Java's {@link HijrahChronology} implementation. It includes conversion methods
 * to and from Gregorian dates.
 * </p>
 * <p>
 * The Hijri calendar is a lunar calendar used in Islamic contexts, including
 * for determining contract dates and profit calculation periods in Islamic finance.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class HijriDate {
    private static final HijrahChronology HIJRI_CHRONOLOGY = HijrahChronology.INSTANCE;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @NotNull
    private final HijrahDate date;

    /**
     * Private constructor.
     *
     * @param date the HijrahDate instance
     */
    private HijriDate(HijrahDate date) {
        this.date = Objects.requireNonNull(date, "Hijri date cannot be null");
    }

    /**
     * Create a HijriDate from year, month, and day in the Hijri calendar.
     *
     * @param year  the Hijri year
     * @param month the Hijri month (1-12)
     * @param day   the Hijri day of month
     * @return a new HijriDate instance
     */
    public static HijriDate of(int year, int month, int day) {
        HijrahDate hijrahDate = HIJRI_CHRONOLOGY.date(year, month, day);
        return new HijriDate(hijrahDate);
    }

    /**
     * Create a HijriDate from a Gregorian LocalDate.
     *
     * @param gregorianDate the Gregorian date
     * @return a new HijriDate instance
     */
    public static HijriDate fromGregorian(LocalDate gregorianDate) {
        Objects.requireNonNull(gregorianDate, "Gregorian date cannot be null");
        HijrahDate hijrahDate = HIJRI_CHRONOLOGY.date(gregorianDate);
        return new HijriDate(hijrahDate);
    }

    /**
     * Create a HijriDate for today in the Hijri calendar.
     *
     * @return a new HijriDate instance representing today
     */
    public static HijriDate now() {
        return fromGregorian(LocalDate.now());
    }

    /**
     * Convert this Hijri date to a Gregorian LocalDate.
     *
     * @return the equivalent Gregorian date
     */
    public LocalDate toGregorian() {
        return LocalDate.from(date);
    }

    /**
     * Get the Hijri year.
     *
     * @return the year in the Hijri calendar
     */
    public int getYear() {
        return date.get(HIJRI_CHRONOLOGY.prolepticYear());
    }

    /**
     * Get the Hijri month (1-12).
     *
     * @return the month number
     */
    public int getMonth() {
        return date.getMonthValue();
    }

    /**
     * Get the day of the month in the Hijri calendar.
     *
     * @return the day of month
     */
    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    /**
     * Add days to this Hijri date.
     *
     * @param days the number of days to add (can be negative)
     * @return a new HijriDate instance
     */
    public HijriDate plusDays(long days) {
        return new HijriDate(date.plus(days, ChronoUnit.DAYS));
    }

    /**
     * Add months to this Hijri date.
     *
     * @param months the number of months to add (can be negative)
     * @return a new HijriDate instance
     */
    public HijriDate plusMonths(long months) {
        return new HijriDate(date.plus(months, ChronoUnit.MONTHS));
    }

    /**
     * Add years to this Hijri date.
     *
     * @param years the number of years to add (can be negative)
     * @return a new HijriDate instance
     */
    public HijriDate plusYears(long years) {
        return new HijriDate(date.plus(years, ChronoUnit.YEARS));
    }

    /**
     * Subtract days from this Hijri date.
     *
     * @param days the number of days to subtract
     * @return a new HijriDate instance
     */
    public HijriDate minusDays(long days) {
        return plusDays(-days);
    }

    /**
     * Subtract months from this Hijri date.
     *
     * @param months the number of months to subtract
     * @return a new HijriDate instance
     */
    public HijriDate minusMonths(long months) {
        return plusMonths(-months);
    }

    /**
     * Subtract years from this Hijri date.
     *
     * @param years the number of years to subtract
     * @return a new HijriDate instance
     */
    public HijriDate minusYears(long years) {
        return plusYears(-years);
    }

    /**
     * Check if this date is before another Hijri date.
     *
     * @param other the other date
     * @return true if this date is before the other date
     */
    public boolean isBefore(HijriDate other) {
        Objects.requireNonNull(other, "Other date cannot be null");
        return this.date.isBefore(other.date);
    }

    /**
     * Check if this date is after another Hijri date.
     *
     * @param other the other date
     * @return true if this date is after the other date
     */
    public boolean isAfter(HijriDate other) {
        Objects.requireNonNull(other, "Other date cannot be null");
        return this.date.isAfter(other.date);
    }

    /**
     * Check if this date is equal to another Hijri date.
     *
     * @param other the other date
     * @return true if the dates are equal
     */
    public boolean isEqual(HijriDate other) {
        Objects.requireNonNull(other, "Other date cannot be null");
        return this.date.isEqual(other.date);
    }

    /**
     * Calculate the number of days between this date and another Hijri date.
     *
     * @param other the other date
     * @return the number of days between the dates (positive if other is after this date)
     */
    public long daysBetween(HijriDate other) {
        Objects.requireNonNull(other, "Other date cannot be null");
        return ChronoUnit.DAYS.between(this.date, other.date);
    }

    /**
     * Calculate the number of months between this date and another Hijri date.
     *
     * @param other the other date
     * @return the number of months between the dates (positive if other is after this date)
     */
    public long monthsBetween(HijriDate other) {
        Objects.requireNonNull(other, "Other date cannot be null");
        return ChronoUnit.MONTHS.between(this.date, other.date);
    }

    /**
     * Get the underlying HijrahDate instance.
     *
     * @return the HijrahDate
     */
    public HijrahDate getDate() {
        return date;
    }

    /**
     * Format this Hijri date as a string in ISO-like format (yyyy-MM-dd).
     *
     * @return formatted date string
     */
    public String format() {
        return date.format(FORMATTER);
    }

    /**
     * Format this Hijri date with a custom pattern.
     *
     * @param pattern the date pattern
     * @return formatted date string
     */
    public String format(String pattern) {
        Objects.requireNonNull(pattern, "Pattern cannot be null");
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HijriDate hijriDate)) return false;
        return date.equals(hijriDate.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d AH", getYear(), getMonth(), getDayOfMonth());
    }
}
