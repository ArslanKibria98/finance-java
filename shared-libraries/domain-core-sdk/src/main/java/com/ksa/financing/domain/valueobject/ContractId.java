package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable Contract identifier value object.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record ContractId(@NotNull UUID value) {

    public ContractId {
        Objects.requireNonNull(value, "ContractId value cannot be null");
    }

    public static ContractId of(UUID value) {
        return new ContractId(value);
    }

    public static ContractId of(String value) {
        Objects.requireNonNull(value, "ContractId string cannot be null");
        return new ContractId(UUID.fromString(value));
    }

    public static ContractId generate() {
        return new ContractId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
