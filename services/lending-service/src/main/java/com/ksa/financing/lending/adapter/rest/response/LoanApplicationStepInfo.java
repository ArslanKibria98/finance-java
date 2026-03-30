package com.ksa.financing.lending.adapter.rest.response;

import com.ksa.financing.lending.domain.model.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single loan application step with display info and completion status.
 * Follows the same pattern as onboarding StepInfo for consistent UI/UX.
 */
@Schema(description = "Individual loan application step with progress status")
public record LoanApplicationStepInfo(

        @Schema(description = "Step order (1-8)")
        int order,

        @Schema(description = "Internal step name (e.g., BASIC_INFORMATION)")
        String name,

        @Schema(description = "User-facing step label")
        String label,

        @Schema(description = "Step description for the user")
        String description,

        @Schema(description = "Step status: completed, current, pending, failed")
        String status
) {

    /**
     * User-facing loan application steps mapped from internal ApplicationStatus.
     * Each step has a "completedAtStatus" which is the ApplicationStatus that must
     * be reached for this user-facing step to be considered complete.
     */
    private static final List<StepDefinition> STEP_DEFINITIONS = List.of(
            new StepDefinition(1, "BASIC_INFORMATION", "Basic Information",
                    "Select product, amount, purpose of finance and tenure",
                    ApplicationStatus.BASIC_INFO_SUBMITTED),
            new StepDefinition(2, "ADD_BANK_ACCOUNT", "Add Bank Account",
                    "Link your bank account for verification and disbursement",
                    ApplicationStatus.BANK_ACCOUNT_VERIFIED),
            new StepDefinition(3, "ELIGIBILITY_CHECK", "Eligibility Check",
                    "Review your eligibility based on credit and affordability",
                    ApplicationStatus.ELIGIBILITY_PASSED),
            new StepDefinition(4, "ACCEPT_OFFER", "Accept Offer",
                    "Review and accept the financing offer with profit rate and schedule",
                    ApplicationStatus.OFFER_ACCEPTED),
            new StepDefinition(5, "SIGN_CONTRACT", "Sign Contract",
                    "Review and sign the financing contract",
                    ApplicationStatus.CONTRACT_SIGNING),
            new StepDefinition(6, "OTP_VERIFICATION", "OTP Verification",
                    "Verify your identity with a one-time password",
                    ApplicationStatus.OTP_VERIFICATION),
            new StepDefinition(7, "CALL_VERIFICATION", "Call Verification",
                    "Authenticate your identity by receiving a verification call",
                    ApplicationStatus.CONTRACT_SIGNED),
            new StepDefinition(8, "FUNDS_TRANSFER", "Funds Transfer",
                    "Loan account created and amount transferred to your bank account",
                    ApplicationStatus.APPROVED)
    );

    /**
     * Builds the list of user-facing steps with completion status.
     *
     * @param currentStatus the current ApplicationStatus
     * @param failedAtStatus when status is REJECTED, indicates which step was in progress
     *                       when rejection occurred (pass null if unknown)
     */
    public static List<LoanApplicationStepInfo> buildSteps(ApplicationStatus currentStatus,
                                                           ApplicationStatus failedAtStatus) {
        List<LoanApplicationStepInfo> steps = new ArrayList<>();

        if (currentStatus == null) {
            currentStatus = ApplicationStatus.DRAFT;
        }

        if (currentStatus == ApplicationStatus.REJECTED) {
            return buildFailedSteps(failedAtStatus);
        }

        if (currentStatus == ApplicationStatus.CANCELLED || currentStatus == ApplicationStatus.EXPIRED) {
            return buildTerminalSteps(currentStatus);
        }

        int currentOrdinal = currentStatus.ordinal();
        int activeOrder = getActiveStepOrder(currentStatus);

        for (StepDefinition def : STEP_DEFINITIONS) {
            String stepStatus;
            if (def.order == activeOrder) {
                stepStatus = "current";
            } else if (def.completedAtStatus.ordinal() <= currentOrdinal) {
                stepStatus = "completed";
            } else {
                stepStatus = "pending";
            }
            steps.add(new LoanApplicationStepInfo(def.order, def.name, def.label, def.description, stepStatus));
        }

        return steps;
    }

    /**
     * Convenience overload when no failedAtStatus is known.
     */
    public static List<LoanApplicationStepInfo> buildSteps(ApplicationStatus currentStatus) {
        return buildSteps(currentStatus, null);
    }

    /**
     * Builds step list for a REJECTED application.
     * Marks completed steps, the failed step, and remaining pending steps.
     */
    private static List<LoanApplicationStepInfo> buildFailedSteps(ApplicationStatus failedAtStatus) {
        List<LoanApplicationStepInfo> steps = new ArrayList<>();
        int failedOrder = failedAtStatus != null ? getActiveStepOrder(failedAtStatus) : 1;

        for (StepDefinition def : STEP_DEFINITIONS) {
            String stepStatus;
            if (def.order < failedOrder) {
                stepStatus = "completed";
            } else if (def.order == failedOrder) {
                stepStatus = "failed";
            } else {
                stepStatus = "pending";
            }
            steps.add(new LoanApplicationStepInfo(def.order, def.name, def.label, def.description, stepStatus));
        }

        return steps;
    }

    /**
     * Builds step list for CANCELLED or EXPIRED applications.
     */
    private static List<LoanApplicationStepInfo> buildTerminalSteps(ApplicationStatus terminalStatus) {
        List<LoanApplicationStepInfo> steps = new ArrayList<>();

        for (StepDefinition def : STEP_DEFINITIONS) {
            steps.add(new LoanApplicationStepInfo(def.order, def.name, def.label, def.description, "pending"));
        }

        return steps;
    }

    /**
     * Returns the next action the client should take based on the current status.
     */
    public static String getNextAction(ApplicationStatus currentStatus) {
        if (currentStatus == null) return "START_APPLICATION";
        return switch (currentStatus) {
            case DRAFT -> "SUBMIT_BASIC_INFO";
            case BASIC_INFO_SUBMITTED -> "ADD_BANK_ACCOUNT";
            case BANK_ACCOUNT_PENDING -> "AWAIT_BANK_VERIFICATION";
            case BANK_ACCOUNT_VERIFIED -> "ELIGIBILITY_CHECK";
            case SIMAH_CONSENT_GIVEN -> "AWAIT_ELIGIBILITY_CHECK";
            case ELIGIBILITY_CHECKING -> "AWAIT_ELIGIBILITY_RESULT";
            case ELIGIBILITY_PASSED -> "AWAIT_OFFER";
            case OFFER_PRESENTED -> "ACCEPT_OR_REJECT_OFFER";
            case OFFER_ACCEPTED -> "AWAIT_CONTRACT";
            case CONTRACT_PENDING -> "SIGN_CONTRACT";
            case CONTRACT_SIGNING -> "SIGN_CONTRACT";
            case OTP_VERIFICATION -> "VERIFY_OTP";
            case IVR_VERIFICATION -> "AWAIT_IVR_CALL";
            case CONTRACT_SIGNED -> "AWAIT_DISBURSEMENT";
            case LOAN_CREATING -> "AWAIT_DISBURSEMENT";
            case DISBURSING -> "AWAIT_FUNDS_TRANSFER";
            case APPROVED -> "DONE";
            case REJECTED -> "APPLICATION_REJECTED";
            case CANCELLED -> "APPLICATION_CANCELLED";
            case EXPIRED -> "APPLICATION_EXPIRED";
            case EXPIRED_RESUMABLE -> "RESUME_CONTRACT_SIGNING";
        };
    }

    /**
     * Maps an ApplicationStatus to its user-facing step order number.
     * SIMAH Consent + Eligibility Check are merged into step 3.
     */
    private static int getActiveStepOrder(ApplicationStatus status) {
        if (status == null) return 1;
        return switch (status) {
            case DRAFT, BASIC_INFO_SUBMITTED -> 1;
            case BANK_ACCOUNT_PENDING -> 2;
            case BANK_ACCOUNT_VERIFIED -> 3;
            case SIMAH_CONSENT_GIVEN, ELIGIBILITY_CHECKING, ELIGIBILITY_PASSED -> 3;
            case OFFER_PRESENTED, OFFER_ACCEPTED -> 4;
            case CONTRACT_PENDING, CONTRACT_SIGNING -> 5;
            case OTP_VERIFICATION -> 6;
            case IVR_VERIFICATION, CONTRACT_SIGNED -> 7;
            case LOAN_CREATING, DISBURSING, APPROVED -> 8;
            case EXPIRED_RESUMABLE -> 5;
            case REJECTED, CANCELLED, EXPIRED -> 1;
        };
    }

    private record StepDefinition(int order, String name, String label, String description,
                                  ApplicationStatus completedAtStatus) {}
}
