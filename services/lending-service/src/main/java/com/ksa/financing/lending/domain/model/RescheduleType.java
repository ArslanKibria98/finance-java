package com.ksa.financing.lending.domain.model;

/**
 * Loan rescheduling types per Blueprint 17.
 *
 * SKIP_PAYMENT       — Move one installment to end of tenure (self-service, no approval, max 2 skips)
 * TENURE_EXTENSION   — Extend loan tenure to reduce installment (max 12 months extension, needs approval)
 * PAYMENT_HOLIDAY    — Pause payments for N months with no additional profit (max 3 months, needs approval + justification)
 * RESTRUCTURING      — Full restructure for distressed loans (credit committee approval, GL write-off entries)
 */
public enum RescheduleType {

    SKIP_PAYMENT,
    TENURE_EXTENSION,
    PAYMENT_HOLIDAY,
    RESTRUCTURING;

    /** Returns true if this type requires a human approval step in the workflow. */
    public boolean requiresApproval() {
        return this == TENURE_EXTENSION || this == PAYMENT_HOLIDAY || this == RESTRUCTURING;
    }

    /** Returns true if GL journal entries must be posted to ledger-service. */
    public boolean requiresGlEntries() {
        return this == RESTRUCTURING;
    }

    /** Returns true if the customer must provide a written justification. */
    public boolean requiresJustification() {
        return this == PAYMENT_HOLIDAY || this == RESTRUCTURING;
    }

    /** Minimum loan age (months) before this reschedule type is allowed. */
    public int minLoanAgeMonths() {
        return switch (this) {
            case SKIP_PAYMENT -> 3;
            case TENURE_EXTENSION -> 6;
            case PAYMENT_HOLIDAY -> 3;
            case RESTRUCTURING -> 0;
        };
    }
}
