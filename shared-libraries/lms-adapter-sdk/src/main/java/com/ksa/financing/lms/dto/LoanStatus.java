package com.ksa.financing.lms.dto;

/**
 * Loan status enumeration representing the loan lifecycle stages.
 */
public enum LoanStatus {
    // Application Stage
    SUBMITTED,
    UNDER_REVIEW,
    PENDING_DOCUMENTATION,

    // Approval Stage
    APPROVED,
    REJECTED,
    WITHDRAWN,

    // Disbursement Stage
    PENDING_DISBURSEMENT,
    PARTIALLY_DISBURSED,
    FULLY_DISBURSED,

    // Active Stage
    ACTIVE,
    IN_ARREARS,
    RESTRUCTURED,

    // Closure Stage
    CLOSED_PREPAID,    // Early settlement with Ibra
    CLOSED_WRITTEN_OFF,
    CLOSED_OBLIGATIONS_MET,
    CLOSED_CANCELLED,

    // Special Status
    SUSPENDED,
    UNDER_LITIGATION
}