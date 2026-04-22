package com.ksa.financing.ledger.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed value object for JournalEntry identity.
 * Pure domain class — zero framework imports.
 */
public record JournalEntryId(UUID value) {

    public JournalEntryId {
        Objects.requireNonNull(value, "JournalEntryId value cannot be null");
    }

    public static JournalEntryId generate() {
        return new JournalEntryId(UUID.randomUUID());
    }

    public static JournalEntryId of(UUID value) {
        return new JournalEntryId(value);
    }

    public static JournalEntryId of(String value) {
        return new JournalEntryId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
