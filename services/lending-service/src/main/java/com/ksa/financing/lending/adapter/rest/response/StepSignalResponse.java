package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Locale;

/**
 * Rich response returned after each signal endpoint call.
 * Includes tracker-style steps list, nextAction, and full workflow data.
 */
@Schema(description = "Response after sending a workflow signal, includes tracker and workflow state")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepSignalResponse(

        @Schema(description = "Signal delivery status: SIGNAL_SENT")
        String signalStatus,

        @Schema(description = "Step that was signaled (e.g. BASIC_INFO, BANK_ACCOUNT)")
        String step,

        @Schema(description = "Application ID")
        String applicationId,

        @Schema(description = "Application number")
        String applicationNumber,

        @Schema(description = "Current application status")
        String currentStep,

        @Schema(description = "Overall status: active, approved, rejected, cancelled, expired")
        String status,

        @Schema(description = "Next action the client should take")
        String nextAction,

        @Schema(description = "Application steps with progress tracking")
        List<LoanApplicationStepInfo> steps,

        @Schema(description = "Pre-qualification / finance calculation data")
        PreQualificationData preQualification,

        @Schema(description = "Current workflow state with all data")
        WorkflowState workflowState

) {

    @Schema(description = "Current workflow state queried from Temporal")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowState(

            @Schema(description = "Current stepper index (1-7)")
            int stepperIndex,

            @Schema(description = "Current step name")
            String stepName,

            @Schema(description = "Application status")
            String status,

            @Schema(description = "Sub-step detail (e.g. AWAITING_INPUT, CREDIT_CHECK)")
            String subStep,

            @Schema(description = "Whether the current step is complete and can proceed")
            boolean canProceed,

            @Schema(description = "Error message if any step failed")
            @JsonInclude(JsonInclude.Include.ALWAYS)
            String errorMessage,

            @Schema(description = "Application ID (UUID)")
            String applicationId,

            @Schema(description = "Application number")
            String applicationNumber,

            @Schema(description = "Basic info data (populated after Step 1)")
            BasicInfoData basicInfo,

            @Schema(description = "Bank account data (populated after Step 2)")
            BankAccountData bankAccount,

            @Schema(description = "Eligibility data (populated after Step 3)")
            EligibilityData eligibility,

            @Schema(description = "Offer details (populated after Step 3)")
            OfferDetails offer,

            @Schema(description = "Contract info (populated after Step 5)")
            ContractInfo contract,

            @Schema(description = "Loan info (populated after disbursement)")
            LoanInfo loan
    ) {
        public static WorkflowState from(ApplicationStatusInfo statusInfo) {
            if (statusInfo == null) return null;
            if ("MANUAL_REVIEW".equals(statusInfo.status())) {
                return new WorkflowState(
                        8,
                        "Application Submitted",
                        statusInfo.status(),
                        "AWAITING_UNDERWRITER_DECISION",
                        true,
                        null,
                        statusInfo.applicationId(),
                        statusInfo.applicationNumber(),
                        statusInfo.basicInfo(),
                        statusInfo.bankAccount(),
                        statusInfo.eligibility(),
                        statusInfo.offer(),
                        statusInfo.contract(),
                        statusInfo.loan()
                );
            }
            return new WorkflowState(
                    statusInfo.stepperIndex(),
                    statusInfo.stepName(),
                    statusInfo.status(),
                    null,
                    true,
                    null,
                    statusInfo.applicationId(),
                    statusInfo.applicationNumber(),
                    statusInfo.basicInfo(),
                    statusInfo.bankAccount(),
                    statusInfo.eligibility(),
                    statusInfo.offer(),
                    statusInfo.contract(),
                    statusInfo.loan()
            );
        }

        public static WorkflowState from(StepInfo stepInfo, ApplicationStatusInfo statusInfo) {
            if (stepInfo == null && statusInfo == null) return null;

            if (statusInfo != null && stepInfo != null) {
                if ("MANUAL_REVIEW".equals(statusInfo.status())) {
                    return new WorkflowState(
                            8,
                            "Application Submitted",
                            statusInfo.status(),
                            "AWAITING_UNDERWRITER_DECISION",
                            true,
                            null,
                            statusInfo.applicationId(),
                            statusInfo.applicationNumber(),
                            statusInfo.basicInfo(),
                            statusInfo.bankAccount(),
                            statusInfo.eligibility(),
                            statusInfo.offer(),
                            statusInfo.contract(),
                            statusInfo.loan()
                    );
                }
                return new WorkflowState(
                        stepInfo.stepperIndex(),
                        stepInfo.stepName(),
                        stepInfo.status(),
                        stepInfo.subStep(),
                        stepInfo.canProceed(),
                        stepInfo.errorMessage(),
                        statusInfo.applicationId(),
                        statusInfo.applicationNumber(),
                        statusInfo.basicInfo(),
                        statusInfo.bankAccount(),
                        statusInfo.eligibility(),
                        statusInfo.offer(),
                        statusInfo.contract(),
                        statusInfo.loan()
                );
            }

            if (stepInfo != null) {
                if ("MANUAL_REVIEW".equals(stepInfo.status())) {
                    return new WorkflowState(
                            8,
                            "Application Submitted",
                            stepInfo.status(),
                            "AWAITING_UNDERWRITER_DECISION",
                            true,
                            null,
                            null, null, null, null, null, null, null, null
                    );
                }
                return new WorkflowState(
                        stepInfo.stepperIndex(),
                        stepInfo.stepName(),
                        stepInfo.status(),
                        stepInfo.subStep(),
                        stepInfo.canProceed(),
                        stepInfo.errorMessage(),
                        null, null, null, null, null, null, null, null
                );
            }

            return from(statusInfo);
        }
    }

    /**
     * Build full response with tracker steps from workflow status info.
     */
    public static StepSignalResponse of(String step, ApplicationStatusInfo statusInfo) {
        var wfState = WorkflowState.from(statusInfo);
        return buildWithSteps(step, statusInfo, wfState);
    }

    public static StepSignalResponse of(String step, StepInfo stepInfo, ApplicationStatusInfo statusInfo) {
        var wfState = WorkflowState.from(stepInfo, statusInfo);
        return buildWithSteps(step, statusInfo, wfState);
    }

    public static StepSignalResponse signalOnly(String step) {
        return new StepSignalResponse("SIGNAL_SENT", step,
                null, null, null, "active", null, null, null, null);
    }

    private static StepSignalResponse buildWithSteps(String step, ApplicationStatusInfo statusInfo,
                                                     WorkflowState wfState) {
        String currentStatusStr = statusInfo != null ? statusInfo.status() : null;
        ApplicationStatus appStatus;
        try {
            appStatus = currentStatusStr != null
                    ? ApplicationStatus.valueOf(currentStatusStr.trim().toUpperCase(Locale.ROOT))
                    : null;
        } catch (IllegalArgumentException e) {
            appStatus = null;
        }

        var trackerSteps = LoanApplicationStepInfo.buildSteps(currentStatusStr);
        var nextAction = LoanApplicationStepInfo.getNextAction(currentStatusStr);
        var overallStatus = resolveOverallStatus(currentStatusStr, appStatus);

        // Calculate preQualification from workflow data (prefer official offer if available)
        PreQualificationData preQual = null;
        if (statusInfo != null) {
            if (statusInfo.offer() != null) {
                preQual = PreQualificationData.fromOffer(statusInfo.offer());
            } else if (statusInfo.basicInfo() != null) {
                var info = statusInfo.basicInfo();
                preQual = PreQualificationData.calculateFinance(
                        info.requestedAmount(),
                        info.requestedTenureMonths(),
                        info.profitRate(),
                        info.processingFeePercent(),
                        info.processingFeeAmount(),
                        info.adminFeeAmount()
                );
            }
        }

        return new StepSignalResponse(
                "SIGNAL_SENT",
                step,
                statusInfo != null ? statusInfo.applicationId() : null,
                statusInfo != null ? statusInfo.applicationNumber() : null,
                currentStatusStr,
                overallStatus,
                nextAction,
                trackerSteps,
                preQual,
                wfState
        );
    }

    private static String resolveOverallStatus(ApplicationStatus status) {
        if (status == null) return "active";
        return switch (status) {
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            case CANCELLED -> "cancelled";
            case EXPIRED -> "expired";
            case EXPIRED_RESUMABLE -> "expired_resumable";
            default -> "active";
        };
    }

    private static String resolveOverallStatus(String rawStatus, ApplicationStatus status) {
        if (rawStatus != null && "MANUAL_REVIEW".equals(rawStatus.trim().toUpperCase(Locale.ROOT))) {
            return "active";
        }
        return resolveOverallStatus(status);
    }
}
