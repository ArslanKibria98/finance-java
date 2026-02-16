package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable Loan identifier value object.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record LoanId(@NotNull UUID value) {

    public LoanId {
        Objects.requireNonNull(value, "LoanId value cannot be null");
    }

    public static LoanId of(UUID value) {
        return new LoanId(value);
    }

    public static LoanId of(String value) {
        Objects.requireNonNull(value, "LoanId string cannot be null");
        return new LoanId(UUID.fromString(value));
    }

    public static LoanId generate() {
        return new LoanId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
