package com.ksa.financing.collections.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class RepaymentScheduleId {

    private final UUID value;

    private RepaymentScheduleId(UUID value) {
        this.value = Objects.requireNonNull(value, "RepaymentScheduleId value must not be null");
    }

    public static RepaymentScheduleId of(UUID value) {
        return new RepaymentScheduleId(value);
    }

    public static RepaymentScheduleId generate() {
        return new RepaymentScheduleId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepaymentScheduleId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
