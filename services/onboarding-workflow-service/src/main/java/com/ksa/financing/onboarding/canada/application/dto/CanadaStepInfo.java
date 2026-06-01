package com.ksa.financing.onboarding.canada.application.dto;

import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;

public final class CanadaStepInfo {

    private CanadaStepInfo() {}

    public static String nextAction(CanadaOnboardingStep step) {
        if (step == null) return "INITIATE";
        return switch (step) {
            case INITIATED, OTP_SENT -> "VERIFY_OTP";
            case OTP_VERIFIED -> "SELECT_DOCUMENT";
            case DOC_SELECTED -> "UPLOAD_DOCUMENT";
            case DOC_VERIFIED -> "CONFIRM_DATA";
            case DOC_CONFIRMED -> "UPLOAD_SELFIE";
            case SELFIE_VERIFIED -> "SET_PIN";
            case PIN_SETUP -> "ENABLE_BIOMETRICS";
            case BIOMETRICS_SETUP, COMPLETED -> "DASHBOARD";
            case FAILED -> "RESTART";
        };
    }
}
