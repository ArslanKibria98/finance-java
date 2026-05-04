package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.ApplicationStatusInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;

@Schema(description = "BRD UC#03 — Finance Application Tracker")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicationTrackerResponse(

        @Schema(description = "Application ID")
        String applicationId,

        @Schema(description = "Application number (human-readable)")
        String applicationNumber,

        @Schema(description = "Requested financing amount in SAR")
        BigDecimal requestedAmount,

        @Schema(description = "Total payable amount in SAR")
        BigDecimal totalPayable,

        @Schema(description = "Current application status (internal)")
        String currentStep,

        @Schema(description = "Overall application status: active, approved, rejected, cancelled, expired")
        String status,

        @Schema(description = "Next action the client should take")
        String nextAction,

        @Schema(description = "Failure/rejection reason (null if not rejected)")
        String failureReason,

        @Schema(description = "Application steps with progress tracking")
        List<LoanApplicationStepInfo> steps,

        @Schema(description = "Pre-qualification / finance calculation data")
        PreQualificationData preQualification,

        @Schema(description = "Application creation timestamp")
        String createdAt,

        @Schema(description = "Last status update timestamp")
        String lastUpdatedAt,

        @Schema(description = "Full workflow state with all step data (basicInfo, bankAccount, eligibility, offer, contract, loan)")
        StepSignalResponse.WorkflowState workflowState
) {

    public static ApplicationTrackerResponse build(
            String applicationId,
            String applicationNumber,
            BigDecimal requestedAmount,
            BigDecimal totalPayable,
            String currentStatusStr,
            String failureReason,
            String createdAt,
            String lastUpdatedAt,
            int tenureMonths,
            BigDecimal profitRate,
            ApplicationStatusInfo statusInfo
    ) {
        ApplicationStatus appStatus = parseStatus(currentStatusStr);
        var steps = LoanApplicationStepInfo.buildSteps(currentStatusStr);
        var nextAction = LoanApplicationStepInfo.getNextAction(currentStatusStr);
        var overallStatus = resolveOverallStatus(currentStatusStr, appStatus);
        
        var preQual = (statusInfo != null && statusInfo.offer() != null)
                ? PreQualificationData.fromOffer(statusInfo.offer())
                : PreQualificationData.calculateFinance(
                        requestedAmount, 
                        tenureMonths, 
                        profitRate,
                        statusInfo != null && statusInfo.basicInfo() != null ? statusInfo.basicInfo().processingFeePercent() : null,
                        statusInfo != null && statusInfo.basicInfo() != null ? statusInfo.basicInfo().processingFeeAmount() : null,
                        statusInfo != null && statusInfo.basicInfo() != null ? statusInfo.basicInfo().adminFeeAmount() : null
                );
        
        var workflowState = statusInfo != null ? StepSignalResponse.WorkflowState.from(statusInfo) : null;

        return new ApplicationTrackerResponse(
                applicationId, applicationNumber,
                requestedAmount, totalPayable,
                currentStatusStr, overallStatus, nextAction,
                failureReason, steps, preQual, createdAt, lastUpdatedAt,
                workflowState
        );
    }

    public static ApplicationTrackerResponse build(LoanApplicationAggregate aggregate) {
        if (aggregate == null) return null;

        var currentStatusStr = aggregate.getStatus().name();
        var appStatus = aggregate.getStatus();
        var steps = LoanApplicationStepInfo.buildSteps(currentStatusStr);
        var nextAction = LoanApplicationStepInfo.getNextAction(currentStatusStr);
        var overallStatus = resolveOverallStatus(currentStatusStr, appStatus);

        // Prefer offered/accepted total payable from aggregate
        BigDecimal totalPayable = aggregate.getOfferedTotalPayable();

        var preQual = PreQualificationData.fromAggregate(aggregate);

        return new ApplicationTrackerResponse(
                aggregate.getId().getValue().toString(),
                aggregate.getApplicationNumber(),
                aggregate.getRequestedAmount(),
                totalPayable,
                currentStatusStr,
                overallStatus,
                nextAction,
                null, // failureReason
                steps,
                preQual,
                aggregate.getCreatedAt() != null ? aggregate.getCreatedAt().toString() : null,
                aggregate.getUpdatedAt() != null ? aggregate.getUpdatedAt().toString() : null,
                null // workflowState not available for DB-based fallback
        );
    }

    public static ApplicationTrackerResponse build(
            String applicationId,
            String applicationNumber,
            BigDecimal requestedAmount,
            BigDecimal totalPayable,
            String currentStatusStr,
            String failureReason,
            String createdAt,
            String lastUpdatedAt,
            int tenureMonths,
            BigDecimal profitRate
    ) {
        return build(applicationId, applicationNumber, requestedAmount, totalPayable,
                currentStatusStr, failureReason, createdAt, lastUpdatedAt, tenureMonths, profitRate, null);
    }

    public static ApplicationTrackerResponse build(
            String applicationId,
            String applicationNumber,
            BigDecimal requestedAmount,
            BigDecimal totalPayable,
            String currentStatusStr
    ) {
        return build(applicationId, applicationNumber, requestedAmount, totalPayable,
                currentStatusStr, null, null, null, 0, null, null);
    }

    private static ApplicationStatus parseStatus(String status) {
        if (status == null) return null;
        try {
            return ApplicationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
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
