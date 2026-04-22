package com.ksa.financing.collections.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class DunningPolicyId {

    private final UUID value;

    private DunningPolicyId(UUID value) {
        this.value = Objects.requireNonNull(value, "DunningPolicy id cannot be null");
    }

    public static DunningPolicyId of(UUID value) {
        return new DunningPolicyId(value);
    }

    public static DunningPolicyId generate() {
        return new DunningPolicyId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DunningPolicyId that)) return false;
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
