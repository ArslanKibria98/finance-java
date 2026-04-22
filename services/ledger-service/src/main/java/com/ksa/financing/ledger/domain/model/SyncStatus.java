package com.ksa.financing.ledger.domain.model;

/**
 * Fineract sync state machine.
 *
 * Transitions:
 *   PENDING → IN_PROGRESS → SYNCED
 *                        ↓
 *                     FAILED → RETRY_SCHEDULED → IN_PROGRESS
 */
public enum SyncStatus {
    PENDING,
    IN_PROGRESS,
    SYNCED,
    FAILED,
    RETRY_SCHEDULED
}
