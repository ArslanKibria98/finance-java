package com.ksa.financing.ledger.domain.model;

/**
 * State machine for journal entry lifecycle.
 *
 * Transitions:
 *   PENDING → POSTED (after balance validation passes)
 *   POSTED  → REVERSED (when a reversal entry is created)
 *   PENDING → FAILED (when posting fails)
 */
public enum EntryStatus {
    PENDING,
    POSTED,
    REVERSED,
    FAILED
}
