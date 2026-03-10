package com.ksa.financing.onboarding.domain.model;

public enum OnboardingStatus {
    INITIATED,
    OTP_SENT,
    OTP_VERIFIED,
    TERMS_PENDING,
    TERMS_ACCEPTED,
    NAFATH_INITIATED,
    NAFATH_VERIFIED,
    INFO_PENDING,
    SCREENING,
    EDD_REQUIRED,
    EDD_SUBMITTED,
    COMPLETING,
    PIN_SETUP,
    COMPLETED,
    FAILED
}
