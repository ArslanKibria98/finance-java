package com.ksa.financing.collections.domain.model;

public enum InstallmentStatus {
    SCHEDULED,
    DUE,
    GRACE_PERIOD,
    OVERDUE,
    PARTIALLY_PAID,
    PAID,
    WAIVED,
    DEFERRED
}
