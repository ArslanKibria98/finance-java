package com.ksa.financing.onboarding.foreign.domain.model;

public enum ForeignOnboardingStep {
    INITIATED,
    OTP_SENT,
    OTP_VERIFIED,
    PASSPORT_UPLOADED,
    DATA_CONFIRMED,
    SELFIE_VERIFIED,
    PIN_SETUP,
    BIOMETRICS_SETUP,
    COMPLETED,
    FAILED
}
