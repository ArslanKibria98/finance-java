package com.ksa.financing.domain.enums;

/**
 * Loan lifecycle status enumeration.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public enum LoanStatus {
    /** Approved but funds not yet disbursed */
    PENDING_DISBURSEMENT,

    /** Loan active, payments ongoing */
    ACTIVE,

    /** Payment overdue (DPD > 0) */
    DELINQUENT,

    /** Defaulted (DPD > 90) */
    DEFAULT,

    /** Terms modified (restructuring/rescheduling) */
    RESTRUCTURED,

    /** Fully paid off */
    SETTLED,

    /** Written off as bad debt */
    WRITTEN_OFF,

    /** Administratively closed */
    CLOSED;

    public boolean isActive() {
        return this == ACTIVE || this == DELINQUENT || this == DEFAULT;
    }

    public boolean canAcceptPayment() {
        return this == ACTIVE || this == DELINQUENT || this == DEFAULT || this == RESTRUCTURED;
    }

    public boolean isFinalState() {
        return this == SETTLED || this == WRITTEN_OFF || this == CLOSED;
    }
}
