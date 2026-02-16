package com.ksa.financing.domain.valueobject;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable Wallet identifier value object.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record WalletId(@NotNull UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "WalletId value cannot be null");
    }

    public static WalletId of(UUID value) {
        return new WalletId(value);
    }

    public static WalletId of(String value) {
        Objects.requireNonNull(value, "WalletId string cannot be null");
        return new WalletId(UUID.fromString(value));
    }

    public static WalletId generate() {
        return new WalletId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
