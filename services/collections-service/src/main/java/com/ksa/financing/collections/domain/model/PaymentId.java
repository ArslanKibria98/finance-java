package com.ksa.financing.collections.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class PaymentId {

    private final UUID value;

    private PaymentId(UUID value) {
        this.value = Objects.requireNonNull(value, "PaymentId value must not be null");
    }

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentId that)) return false;
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
