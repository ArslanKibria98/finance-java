package com.ksa.financing.risk.domain.model;

public enum RiskAssessmentStatus {
    INITIATED,
    NID_VALIDATED,
    CIF_CHECKED,
    MOBILE_CHECKED,
    BLACKLIST_CHECKED,
    FRAUD_CHECKED,
    DEVICE_CHECKED,
    VELOCITY_CHECKED,
    ACCOUNT_LOCK_CHECKED,
    SANCTIONS_CHECKED,
    RISK_SCORED,
    COMPLETED,
    BLOCKED,
    FAILED
}
