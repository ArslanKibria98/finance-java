package com.ksa.financing.onboarding.canada.domain.model;

public enum CanadaOnboardingStep {
    INITIATED,
    OTP_SENT,
    OTP_VERIFIED,
    DOC_SELECTED,
    DOC_VERIFIED,
    DOC_CONFIRMED,
    SELFIE_VERIFIED,
    PIN_SETUP,
    BIOMETRICS_SETUP,
    COMPLETED,
    FAILED
}
