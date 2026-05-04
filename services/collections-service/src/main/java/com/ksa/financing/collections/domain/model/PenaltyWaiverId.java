package com.ksa.financing.collections.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class PenaltyWaiverId {

    private final UUID value;

    private PenaltyWaiverId(UUID value) {
        this.value = Objects.requireNonNull(value, "PenaltyWaiverId value cannot be null");
    }

    public static PenaltyWaiverId generate() { return new PenaltyWaiverId(UUID.randomUUID()); }
    public static PenaltyWaiverId of(UUID value) { return new PenaltyWaiverId(value); }

    public UUID getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PenaltyWaiverId that)) return false;
        return value.equals(that.value);
    }

    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
