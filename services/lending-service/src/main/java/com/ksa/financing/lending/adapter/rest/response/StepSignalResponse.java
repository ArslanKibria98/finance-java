package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Rich response returned after each signal endpoint call.
 * Queries the Temporal workflow to return actual state after signal delivery.
 */
@Schema(description = "Response after sending a workflow signal, includes current workflow state")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StepSignalResponse(

        @Schema(description = "Signal delivery status: SIGNAL_SENT")
        String signalStatus,

        @Schema(description = "Step that was signaled (e.g. BASIC_INFO, BANK_ACCOUNT)")
        String step,

        @Schema(description = "Current workflow state after signal")
        WorkflowState workflowState

) {

    @Schema(description = "Current workflow state queried from Temporal")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkflowState(

            @Schema(description = "Current stepper index (1-5)")
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

    public static StepSignalResponse of(String step, ApplicationStatusInfo statusInfo) {
        return new StepSignalResponse("SIGNAL_SENT", step,
                WorkflowState.from(statusInfo));
    }

    public static StepSignalResponse of(String step, StepInfo stepInfo, ApplicationStatusInfo statusInfo) {
        return new StepSignalResponse("SIGNAL_SENT", step,
                WorkflowState.from(stepInfo, statusInfo));
    }

    public static StepSignalResponse signalOnly(String step) {
        return new StepSignalResponse("SIGNAL_SENT", step, null);
    }
}
