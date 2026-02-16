package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable Money value object using JSR 354 Money API.
 * <p>
 * IMPORTANT: All monetary amounts in KSA platform use SAR currency with 2 decimal places.
 * This wrapper ensures consistent rounding and currency handling.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class SarMoney {
    private static final CurrencyUnit SAR = Monetary.getCurrency("SAR");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @NotNull
    private final MonetaryAmount amount;

    private SarMoney(MonetaryAmount amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (!SAR.equals(amount.getCurrency())) {
            throw new IllegalArgumentException("Only SAR currency is supported. Got: " + amount.getCurrency());
        }
        // Ensure 2 decimal places
        this.amount = Money.of(
                amount.getNumber().numberValue(BigDecimal.class).setScale(SCALE, ROUNDING_MODE),
                SAR
        );
    }

    /**
     * Create Money from BigDecimal amount in SAR.
     */
    public static SarMoney of(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        return new SarMoney(Money.of(amount, SAR));
    }

    /**
     * Create Money from double amount in SAR.
     */
    public static SarMoney of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    /**
     * Create Money from long amount in SAR.
     */
    public static SarMoney of(long amount) {
        return of(BigDecimal.valueOf(amount));
    }

    /**
     * Zero SAR.
     */
    public static SarMoney zero() {
        return of(BigDecimal.ZERO);
    }

    /**
     * Add another Money amount.
     */
    public SarMoney add(SarMoney other) {
        Objects.requireNonNull(other, "Other amount cannot be null");
        return new SarMoney(this.amount.add(other.amount));
    }

    /**
     * Subtract another Money amount.
     */
    public SarMoney subtract(SarMoney other) {
        Objects.requireNonNull(other, "Other amount cannot be null");
        return new SarMoney(this.amount.subtract(other.amount));
    }

    /**
     * Multiply by a factor.
     */
    public SarMoney multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor cannot be null");
        return new SarMoney(this.amount.multiply(factor));
    }

    /**
     * Multiply by a double factor.
     */
    public SarMoney multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * Divide by a divisor.
     */
    public SarMoney divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new SarMoney(this.amount.divide(divisor));
    }

    /**
     * Divide by an integer divisor.
     */
    public SarMoney divide(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return divide(BigDecimal.valueOf(divisor));
    }

    /**
     * Check if this amount is zero.
     */
    public boolean isZero() {
        return amount.isZero();
    }

    /**
     * Check if this amount is positive.
     */
    public boolean isPositive() {
        return amount.isPositive();
    }

    /**
     * Check if this amount is negative.
     */
    public boolean isNegative() {
        return amount.isNegative();
    }

    /**
     * Check if this amount is greater than another.
     */
    public boolean isGreaterThan(SarMoney other) {
        Objects.requireNonNull(other, "Other amount cannot be null");
        return this.amount.isGreaterThan(other.amount);
    }

    /**
     * Check if this amount is less than another.
     */
    public boolean isLessThan(SarMoney other) {
        Objects.requireNonNull(other, "Other amount cannot be null");
        return this.amount.isLessThan(other.amount);
    }

    /**
     * Check if this amount is greater than or equal to another.
     */
    public boolean isGreaterThanOrEqualTo(SarMoney other) {
        Objects.requireNonNull(other, "Other amount cannot be null");
        return this.amount.isGreaterThanOrEqualTo(other.amount);
    }

    /**
     * Get the underlying MonetaryAmount.
     */
    public MonetaryAmount getAmount() {
        return amount;
    }

    /**
     * Get the numeric value as BigDecimal.
     */
    public BigDecimal getValue() {
        return amount.getNumber().numberValue(BigDecimal.class);
    }

    /**
     * Get the currency unit (always SAR).
     */
    public CurrencyUnit getCurrency() {
        return SAR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SarMoney sarMoney)) return false;
        return amount.equals(sarMoney.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }

    /**
     * Format as string with currency symbol (e.g., "SAR 1,234.56").
     */
    public String toFormattedString() {
        return String.format("SAR %,.2f", getValue());
    }
}
