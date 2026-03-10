package com.ksa.financing.lending.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class LoanId {

    private final UUID value;

    private LoanId(UUID value) {
        this.value = Objects.requireNonNull(value, "LoanId value cannot be null");
    }

    public static LoanId generate() {
        return new LoanId(UUID.randomUUID());
    }

    public static LoanId of(UUID value) {
        return new LoanId(value);
    }

    public static LoanId of(String value) {
        return new LoanId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanId that = (LoanId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
