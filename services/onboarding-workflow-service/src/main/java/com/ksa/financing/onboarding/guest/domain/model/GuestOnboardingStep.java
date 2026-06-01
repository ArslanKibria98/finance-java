package com.ksa.financing.onboarding.guest.domain.model;

public enum GuestOnboardingStep {
    INITIATED,
    OTP_SENT,
    OTP_VERIFIED,
    BIOMETRICS_SETUP,
    PIN_SETUP,
    COMPLETED,
    FAILED
}
