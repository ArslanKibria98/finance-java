package com.ksa.financing.ledger.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object wrapping a SAR monetary amount.
 * All arithmetic uses BigDecimal with RoundingMode.HALF_UP (6 decimal places).
 * Pure domain class — zero framework imports.
 */
public record Money(BigDecimal amount) {

    public static final String CURRENCY = "SAR";
    public static final int SCALE = 6;

    public Money {
        Objects.requireNonNull(amount, "Money amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }
        // Normalize scale
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount).abs());
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean equals(Money other) {
        if (other == null) return false;
        return this.amount.compareTo(other.amount) == 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + CURRENCY;
    }
}
