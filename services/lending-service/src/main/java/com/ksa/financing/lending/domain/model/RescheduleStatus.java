package com.ksa.financing.lending.domain.model;

/**
 * State machine for loan reschedule requests.
 *
 * PENDING      → awaiting approval (or auto-approved for SKIP_PAYMENT)
 * PROCESSING   → approved by manager/credit committee, workflow proceeding
 * APPROVED     → new schedule generated and saved, Fineract synced (terminal success)
 * REJECTED     → rejected by approver or failed eligibility
 * CANCELLED    → cancelled before approval
 */
public enum RescheduleStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    REJECTED,
    CANCELLED
}
