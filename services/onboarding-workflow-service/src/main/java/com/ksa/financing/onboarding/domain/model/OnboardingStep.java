package com.ksa.financing.onboarding.domain.model;

public enum OnboardingStep {
    INITIATED,
    OTP_SENT,
    OTP_VERIFIED,
    TERMS_PENDING,
    TERMS_ACCEPTED,
    NAFATH_INITIATED,
    NAFATH_VERIFIED,
    INFO_PENDING,
    SCREENING,          // PEP & Sanctions screening in progress
    EDD_REQUIRED,       // PEP detected, waiting for EDD form
    EDD_SUBMITTED,      // EDD form submitted, processing risk decision
    COMPLETING,
    PIN_SETUP,          // Waiting for customer to set app PIN
    COMPLETED,
    FAILED
}
