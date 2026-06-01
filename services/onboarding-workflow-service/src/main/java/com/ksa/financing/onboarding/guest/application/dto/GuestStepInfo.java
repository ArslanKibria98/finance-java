package com.ksa.financing.onboarding.guest.application.dto;

import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;

public final class GuestStepInfo {

    private GuestStepInfo() {}

    public static String nextAction(GuestOnboardingStep step) {
        if (step == null) return "INITIATE";
        return switch (step) {
            case INITIATED, OTP_SENT -> "VERIFY_OTP";
            case OTP_VERIFIED -> "ENABLE_BIOMETRICS";
            case BIOMETRICS_SETUP -> "SET_PIN";
            case PIN_SETUP, COMPLETED -> "DASHBOARD";
            case FAILED -> "RESTART";
        };
    }
}
