package com.ksa.financing.collections.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class WriteOffId {

    private final UUID value;

    private WriteOffId(UUID value) {
        this.value = Objects.requireNonNull(value, "WriteOffId value cannot be null");
    }

    public static WriteOffId generate() { return new WriteOffId(UUID.randomUUID()); }
    public static WriteOffId of(UUID value) { return new WriteOffId(value); }

    public UUID getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WriteOffId that)) return false;
        return value.equals(that.value);
    }

    @Override public int hashCode() { return value.hashCode(); }
    @Override public String toString() { return value.toString(); }
}
