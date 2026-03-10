package com.ksa.financing.risk.domain.model;

public enum InternalCheckStep {
    INITIATED,
    NID_VALIDATED,
    BLACKLIST_CHECKED,
    FRAUD_CHECKED,
    DEVICE_CHECKED,
    VELOCITY_CHECKED,
    ACCOUNT_LOCK_CHECKED,
    SANCTIONS_CHECKED,
    RISK_SCORED,
    CIF_CHECKED,
    MOBILE_CHECKED,
    COMPLETED,
    BLOCKED,
    FAILED
}
