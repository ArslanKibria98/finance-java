package com.ksa.financing.onboarding.foreign.application.dto;

import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;

public final class ForeignStepInfo {

    private ForeignStepInfo() {}

    public static String nextAction(ForeignOnboardingStep step) {
        if (step == null) return "INITIATE";
        return switch (step) {
            case INITIATED, OTP_SENT -> "VERIFY_OTP";
            case OTP_VERIFIED -> "UPLOAD_PASSPORT";
            case PASSPORT_UPLOADED -> "CONFIRM_DATA";
            case DATA_CONFIRMED -> "UPLOAD_SELFIE";
            case SELFIE_VERIFIED -> "SET_PIN";
            case PIN_SETUP -> "ENABLE_BIOMETRICS";
            case BIOMETRICS_SETUP, COMPLETED -> "DASHBOARD";
            case FAILED -> "RESTART";
        };
    }
}
