package com.ksa.financing.lending.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class LoanApplicationId {

    private final UUID value;

    private LoanApplicationId(UUID value) {
        this.value = Objects.requireNonNull(value, "LoanApplicationId value cannot be null");
    }

    public static LoanApplicationId generate() {
        return new LoanApplicationId(UUID.randomUUID());
    }

    public static LoanApplicationId of(UUID value) {
        return new LoanApplicationId(value);
    }

    public static LoanApplicationId of(String value) {
        return new LoanApplicationId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanApplicationId that = (LoanApplicationId) o;
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
