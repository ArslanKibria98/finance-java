package com.ksa.financing.collections.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SettlementId(UUID value) {
    public SettlementId {
        Objects.requireNonNull(value, "Settlement ID value cannot be null");
    }

    public static SettlementId generate() {
        return new SettlementId(UUID.randomUUID());
    }

    public static SettlementId of(UUID value) {
        return new SettlementId(value);
    }

    public static SettlementId of(String value) {
        return new SettlementId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
