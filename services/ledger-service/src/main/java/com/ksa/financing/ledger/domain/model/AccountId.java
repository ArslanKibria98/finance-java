package com.ksa.financing.ledger.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed value object for GL Account identity.
 * Pure domain class — zero framework imports.
 */
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "AccountId value cannot be null");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
