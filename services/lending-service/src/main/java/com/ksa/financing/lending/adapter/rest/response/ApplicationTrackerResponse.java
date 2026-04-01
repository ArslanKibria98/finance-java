package com.ksa.financing.lending.adapter.rest.response;

import com.ksa.financing.lending.domain.model.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "BRD UC#03 — Finance Application Tracker")
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
        String lastUpdatedAt
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
            BigDecimal profitRate
    ) {
        ApplicationStatus appStatus = parseStatus(currentStatusStr);
        var steps = LoanApplicationStepInfo.buildSteps(appStatus);
        var nextAction = LoanApplicationStepInfo.getNextAction(appStatus);
        var overallStatus = resolveOverallStatus(appStatus);
        var preQual = PreQualificationData.calculate(requestedAmount, tenureMonths, profitRate);

        return new ApplicationTrackerResponse(
                applicationId, applicationNumber,
                requestedAmount, totalPayable,
                currentStatusStr, overallStatus, nextAction,
                failureReason, steps, preQual, createdAt, lastUpdatedAt
        );
    }

    public static ApplicationTrackerResponse build(
            String applicationId,
            String applicationNumber,
            BigDecimal requestedAmount,
            BigDecimal totalPayable,
            String currentStatusStr
    ) {
        return build(applicationId, applicationNumber, requestedAmount, totalPayable,
                currentStatusStr, null, null, null, 0, null);
    }

    private static ApplicationStatus parseStatus(String status) {
        if (status == null) return null;
        try {
            return ApplicationStatus.valueOf(status);
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
}
