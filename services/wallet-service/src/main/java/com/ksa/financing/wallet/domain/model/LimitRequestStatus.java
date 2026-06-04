package com.ksa.financing.wallet.domain.model;

/**
 * Lifecycle of a customer-initiated wallet transaction-limit change request.
 * PENDING → APPROVED | REJECTED (by an admin), or CANCELLED (by the requester).
 */
public enum LimitRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
