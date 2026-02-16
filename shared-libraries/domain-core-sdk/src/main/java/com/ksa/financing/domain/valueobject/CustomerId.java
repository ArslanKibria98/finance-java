package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable Customer identifier value object.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record CustomerId(@NotNull UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value cannot be null");
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId of(String value) {
        Objects.requireNonNull(value, "CustomerId string cannot be null");
        return new CustomerId(UUID.fromString(value));
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
