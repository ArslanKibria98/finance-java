package com.ksa.financing.onboarding.application.dto;

import com.ksa.financing.onboarding.domain.model.OnboardingStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single onboarding step with its display information and completion status.
 */
public record StepInfo(
    int order,
    String name,
    String label,
    String description,
    String status
) {

    /**
     * User-facing onboarding steps (groups internal workflow steps for clarity).
     * Each step has a "completedAtStep" which is the workflow step that must be reached
     * for this user-facing step to be considered complete.
     */
    private static final List<StepDefinition> STEP_DEFINITIONS = List.of(
        new StepDefinition(1, "MOBILE_VERIFICATION", "Mobile Verification",
                "Verify mobile number ownership via Tahakuk",
                OnboardingStep.OTP_SENT),
        new StepDefinition(2, "OTP_VERIFICATION", "OTP Verification",
                "Verify the OTP code sent to your mobile",
                OnboardingStep.OTP_VERIFIED),
        new StepDefinition(3, "TERMS_AND_CONDITIONS", "Terms & Conditions",
                "Review and accept terms and conditions",
                OnboardingStep.TERMS_ACCEPTED),
        new StepDefinition(4, "NAFATH_VERIFICATION", "Nafath Identity Verification",
                "Verify your identity using the Nafath app",
                OnboardingStep.INFO_PENDING),
        new StepDefinition(5, "ADDITIONAL_INFO", "Additional Information",
                "Submit employment and banking details",
                OnboardingStep.SCREENING),
        new StepDefinition(6, "PEP_SCREENING", "Security Screening",
                "PEP & sanctions screening and risk assessment",
                OnboardingStep.COMPLETING),
        new StepDefinition(7, "PIN_SETUP", "Set App PIN",
                "Create a 6-digit PIN to secure your account",
                OnboardingStep.COMPLETED),
        new StepDefinition(8, "COMPLETION", "Onboarding Complete",
                "Account setup and onboarding finalization",
                OnboardingStep.COMPLETED)
    );

    /**
     * Builds the list of user-facing steps with completion status.
     *
     * @param currentStep the current workflow step
     * @param failedAtStep when currentStep is FAILED, this indicates which step was in progress
     *                     when failure occurred (pass null if unknown — first incomplete step will be marked failed)
     */
    public static List<StepInfo> buildSteps(OnboardingStep currentStep, OnboardingStep failedAtStep) {
        List<StepInfo> steps = new ArrayList<>();

        if (currentStep == null) {
            currentStep = OnboardingStep.INITIATED;
        }

        if (currentStep == OnboardingStep.FAILED) {
            return buildFailedSteps(failedAtStep);
        }

        int currentOrdinal = currentStep.ordinal();
        int activeOrder = getActiveStepOrder(currentStep);

        for (StepDefinition def : STEP_DEFINITIONS) {
            String stepStatus;
            if (def.completedAtStep.ordinal() <= currentOrdinal) {
                stepStatus = "completed";
            } else if (def.order == activeOrder) {
                stepStatus = "current";
            } else {
                stepStatus = "pending";
            }
            steps.add(new StepInfo(def.order, def.name, def.label, def.description, stepStatus));
        }

        return steps;
    }

    /**
     * Convenience overload when no failedAtStep is known.
     */
    public static List<StepInfo> buildSteps(OnboardingStep currentStep) {
        return buildSteps(currentStep, null);
    }

    /**
     * Builds step list for a FAILED workflow. Marks completed steps, the failed step, and pending steps.
     */
    private static List<StepInfo> buildFailedSteps(OnboardingStep failedAtStep) {
        List<StepInfo> steps = new ArrayList<>();
        int failedOrder = failedAtStep != null ? getActiveStepOrder(failedAtStep) : 1;

        for (StepDefinition def : STEP_DEFINITIONS) {
            String stepStatus;
            if (def.order < failedOrder) {
                stepStatus = "completed";
            } else if (def.order == failedOrder) {
                stepStatus = "failed";
            } else {
                stepStatus = "pending";
            }
            steps.add(new StepInfo(def.order, def.name, def.label, def.description, stepStatus));
        }

        return steps;
    }

    /**
     * Returns the next action the client should take based on the current step.
     */
    public static String getNextAction(OnboardingStep currentStep) {
        if (currentStep == null) return "AWAIT_INITIATION";
        return switch (currentStep) {
            case INITIATED -> "AWAIT_INITIATION";
            case OTP_SENT -> "VERIFY_OTP";
            case OTP_VERIFIED, TERMS_PENDING -> "ACCEPT_TERMS";
            case TERMS_ACCEPTED -> "INITIATE_NAFATH";
            case NAFATH_INITIATED -> "AWAIT_NAFATH_CALLBACK";
            case NAFATH_VERIFIED -> "AWAIT_VERIFICATION";
            case INFO_PENDING -> "SUBMIT_ADDITIONAL_INFO";
            case SCREENING -> "AWAIT_SCREENING";
            case EDD_REQUIRED -> "SUBMIT_EDD_FORM";
            case EDD_SUBMITTED -> "AWAIT_RISK_DECISION";
            case COMPLETING -> "SET_PIN";
            case PIN_SETUP -> "SET_PIN";
            case COMPLETED -> "LOGIN";
            case FAILED -> "RETRY_OR_CONTACT_SUPPORT";
        };
    }

    /**
     * Maps an internal workflow step to its user-facing step order number.
     */
    private static int getActiveStepOrder(OnboardingStep step) {
        if (step == null) return 1;
        return switch (step) {
            case INITIATED, OTP_SENT -> 1;
            case OTP_VERIFIED -> 2;
            case TERMS_PENDING, TERMS_ACCEPTED -> 3;
            case NAFATH_INITIATED, NAFATH_VERIFIED -> 4;
            case INFO_PENDING -> 5;
            case SCREENING, EDD_REQUIRED, EDD_SUBMITTED -> 6;
            case COMPLETING -> 7;
            case PIN_SETUP -> 7;
            case COMPLETED -> 8;
            case FAILED -> 1;
        };
    }

    private record StepDefinition(int order, String name, String label, String description,
                                  OnboardingStep completedAtStep) {}
}
