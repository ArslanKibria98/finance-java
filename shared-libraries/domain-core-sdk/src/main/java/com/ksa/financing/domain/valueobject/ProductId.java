package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable Product identifier value object.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record ProductId(@NotNull UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId value cannot be null");
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    public static ProductId of(String value) {
        Objects.requireNonNull(value, "ProductId string cannot be null");
        return new ProductId(UUID.fromString(value));
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
