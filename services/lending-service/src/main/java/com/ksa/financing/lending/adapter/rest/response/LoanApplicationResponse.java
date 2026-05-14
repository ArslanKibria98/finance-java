package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ksa.financing.lending.application.dto.LoanApplicationDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

@Schema(description = "Loan application response with stepper state")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoanApplicationResponse(
        String id,
        String applicationNumber,
        String customerId,
        String nationalId,
        String status,
        @Schema(description = "Display status for list view: IN_PROGRESS for active applications, APPROVED/REJECTED/CANCELLED/EXPIRED for terminal ones")
        String displayStatus,
        int stepperIndex,
        String stepperLabel,
        String workflowId,

        // Pre-qualification
        @Schema(description = "Monthly income in SAR")
        BigDecimal monthlyIncome,
        @Schema(description = "Total monthly expenses")
        BigDecimal totalExpenses,
        @Schema(description = "Existing monthly liabilities")
        BigDecimal existingLiabilities,

        // Individual expense categories
        @Schema(description = "Food & groceries monthly expense")
        BigDecimal foodGroceries,
        @Schema(description = "Utilities monthly expense")
        BigDecimal utilities,
        @Schema(description = "Healthcare monthly expense")
        BigDecimal healthcare,
        @Schema(description = "Communication monthly expense")
        BigDecimal communication,
        @Schema(description = "Housing/rent monthly expense")
        BigDecimal housingRent,
        @Schema(description = "Clothing & essentials monthly expense")
        BigDecimal clothingEssentials,
        @Schema(description = "Education monthly expense")
        BigDecimal education,
        @Schema(description = "Transportation monthly expense")
        BigDecimal transportation,

        // Step 1: Basic Info
        String productId,
        String productCode,
        String productName,
        String shariaStructure,
        BigDecimal requestedAmount,
        int requestedTenureMonths,
        String purposeOfFinance,
        String purposeOfFinanceOther,
        @Schema(description = "Annual profit rate (e.g. 0.12 = 12%)")
        BigDecimal profitRate,
        @Schema(description = "Annual Percentage Rate")
        BigDecimal apr,

        // SafeWatch AML
        @Schema(description = "SafeWatch AML session ID")
        String safeWatchSessionId,
        @Schema(description = "SafeWatch screening status: CLEAR, MATCH, PENDING")
        String safeWatchStatus,

        // Masdar Employment
        @Schema(description = "Employer name from Masdar/GOSI")
        String employerName,
        @Schema(description = "Employment sector")
        String employmentSector,
        @Schema(description = "Employment status: ACTIVE, INACTIVE")
        String employmentStatus,
        @Schema(description = "Basic salary from Masdar")
        BigDecimal basicSalary,
        @Schema(description = "Total salary from Masdar")
        BigDecimal totalSalary,
        @Schema(description = "Employment start date")
        String employmentStartDate,

        // AML Declaration
        @Schema(description = "AML self-declaration completed")
        boolean amlDeclarationCompleted,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime amlDeclarationAt,

        // Step 2: Bank Account
        String disbursementBankCode,
        String disbursementBankName,
        String disbursementIban,
        String disbursementAccountHolder,
        boolean ibanVerified,

        // Step 3: Credit Check & Eligibility
        boolean simahConsent,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime simahConsentAt,
        int creditScore,
        String simahReferenceId,
        BigDecimal verifiedSalary,
        BigDecimal maxEligibleAmount,

        // Step 4: Offer
        BigDecimal offeredAmount,
        BigDecimal offeredMonthlyInstallment,
        BigDecimal offeredTotalProfit,
        BigDecimal offeredTotalPayable,
        BigDecimal processingFee,
        BigDecimal adminFee,
        BigDecimal acceptedAmount,

        // Step 5: Contract & Verification
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime contractExpiresAt,
        boolean otpVerified,
        int otpAttempts,
        boolean ivrVerified,
        int ivrAttempts,

        // NABA + PaymentGuard
        boolean nabaNotificationSent,
        String paymentGuardStatus,

        // Audit
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,

        // Loan / Disbursement
        @Schema(description = "Loan ID (null if not yet created)")
        String loanId,
        @Schema(description = "Loan number")
        String loanNumber,
        @Schema(description = "Loan status: PENDING_DISBURSEMENT, ACTIVE, CLOSED, etc.")
        String loanStatus,
        @Schema(description = "Disbursed principal amount")
        BigDecimal principalAmount,
        @Schema(description = "Total payable amount")
        BigDecimal totalAmount,
        @Schema(description = "Total payable amount (explicit key)")
        BigDecimal totalPayable,
        @Schema(description = "Monthly installment amount")
        BigDecimal installmentAmount,
        @Schema(description = "Fineract loan ID")
        String fineractLoanId,
        @Schema(description = "Disbursement date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        java.time.LocalDate disbursementDate,

        // Current schedule (updated after reschedule — differs from requestedTenureMonths which is the original request)
        @Schema(description = "Current tenure months from loans table (updated after reschedule)")
        Integer currentTenureMonths,
        @Schema(description = "Current maturity date from loans table (updated after reschedule)")
        @JsonFormat(pattern = "yyyy-MM-dd")
        java.time.LocalDate currentMaturityDate,
        @Schema(description = "Current profit rate (updated after restructure)")
        BigDecimal currentProfitRate,

        // Rescheduling
        @Schema(description = "Latest reschedule request ID (if any active reschedule exists)")
        String rescheduleId,
        @Schema(description = "Latest reschedule status: PENDING, AWAITING_APPROVAL, PROCESSING, APPROVED, REJECTED, CANCELLED")
        String rescheduleStatus,
        @Schema(description = "Latest reschedule type")
        String rescheduleType,
        @Schema(description = "Latest reschedule request date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime rescheduleRequestedAt,
        @Schema(description = "Latest reschedule application date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime rescheduleAppliedAt,
        @Schema(description = "Reschedule rejection reason or justification")
        String rescheduleReason,
        @Schema(description = "Detailed summary of the latest reschedule")
        String rescheduleDetails,
        @Schema(description = "True if the loan currently qualifies for early settlement (sourced from collections-service)")
        Boolean earlySettlementEligible
) {
    public static LoanApplicationResponse from(LoanApplicationDto dto) {
        return from(dto, null, null, null, null, null, null, null, null);
    }

    public static LoanApplicationResponse from(
            LoanApplicationDto dto,
            String rescheduleId,
            String rescheduleStatus,
            String rescheduleType,
            LocalDateTime rescheduleRequestedAt,
            LocalDateTime rescheduleAppliedAt,
            String rescheduleReason,
            String rescheduleDetails,
            BigDecimal preliminaryTotalPayable) {
        var normalizedStatus = normalizeStatus(dto.status());
        String displayStatus = deriveDisplayStatus(dto, normalizedStatus);
        
        // Prefer the current loan totalAmount when it exists — it reflects post-reschedule
        // values (tenure extension recomputes total = newInstallment × newTenure).
        // Fall back to offeredTotalPayable for applications that haven't booked a loan yet.
        BigDecimal finalTotalPayable = dto.totalAmount() != null ? dto.totalAmount() : dto.offeredTotalPayable();
        if (finalTotalPayable == null) {
            finalTotalPayable = preliminaryTotalPayable;
        } else if (dto.totalAmount() == null) {
            // Legacy fix only applies when falling back to offeredTotalPayable (pre-loan stage).
            BigDecimal safeAmount = dto.acceptedAmount() != null ? dto.acceptedAmount() :
                                    (dto.offeredAmount() != null ? dto.offeredAmount() : BigDecimal.ZERO);
            BigDecimal safeProfit = dto.offeredTotalProfit() != null ? dto.offeredTotalProfit() : BigDecimal.ZERO;
            BigDecimal safeProcFee = dto.processingFee() != null ? dto.processingFee() : BigDecimal.ZERO;
            BigDecimal safeAdminFee = dto.adminFee() != null ? dto.adminFee() : BigDecimal.ZERO;

            BigDecimal expectedTotal = safeAmount.add(safeProfit).add(safeProcFee).add(safeAdminFee);

            if (safeAmount.compareTo(BigDecimal.ZERO) > 0 && finalTotalPayable.compareTo(expectedTotal) < 0) {
                finalTotalPayable = expectedTotal;
            }
        }

        // Self-heal stale loan.total_amount rows that pre-date the reschedule recompute fix.
        // If installment × tenure disagrees with the persisted totalAmount, trust the schedule.
        if (dto.installmentAmount() != null && dto.currentTenureMonths() != null
                && dto.currentTenureMonths() > 0) {
            BigDecimal scheduledTotal = dto.installmentAmount()
                    .multiply(BigDecimal.valueOf(dto.currentTenureMonths()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (finalTotalPayable == null
                    || finalTotalPayable.setScale(2, RoundingMode.HALF_UP)
                            .compareTo(scheduledTotal) != 0) {
                finalTotalPayable = scheduledTotal;
            }
        }

        // Derive offeredTotalProfit from finalTotalPayable so it stays consistent
        // after a tenure extension / reschedule.
        BigDecimal displayedTotalProfit = dto.offeredTotalProfit();
        if (finalTotalPayable != null && dto.principalAmount() != null) {
            BigDecimal procFee = dto.processingFee() != null ? dto.processingFee() : BigDecimal.ZERO;
            BigDecimal adminFee = dto.adminFee() != null ? dto.adminFee() : BigDecimal.ZERO;
            displayedTotalProfit = finalTotalPayable
                    .subtract(dto.principalAmount())
                    .subtract(procFee)
                    .subtract(adminFee)
                    .max(BigDecimal.ZERO);
        }

        return new LoanApplicationResponse(
                dto.id(),
                dto.applicationNumber(),
                dto.customerId(),
                dto.nationalId(),
                dto.status(),
                displayStatus,
                dto.stepperIndex(),
                dto.stepperLabel(),
                dto.workflowId(),
                scale2(dto.monthlyIncome()),
                scale2(dto.totalExpenses()),
                scale2(dto.existingLiabilities()),
                // Individual expenses
                scale2(dto.foodGroceries()),
                scale2(dto.utilities()),
                scale2(dto.healthcare()),
                scale2(dto.communication()),
                scale2(dto.housingRent()),
                scale2(dto.clothingEssentials()),
                scale2(dto.education()),
                scale2(dto.transportation()),
                // Basic info
                dto.productId(),
                dto.productCode(),
                dto.productName(),
                dto.shariaStructure(),
                scale2(dto.requestedAmount()),
                dto.currentTenureMonths() != null ? dto.currentTenureMonths() : dto.requestedTenureMonths(),
                dto.purposeOfFinance(),
                dto.purposeOfFinanceOther(),
                dto.currentProfitRate() != null ? normalizeProfitRate(dto.currentProfitRate()) : dto.profitRate(),
                dto.apr(),
                // SafeWatch
                dto.safeWatchSessionId(),
                dto.safeWatchStatus(),
                // Masdar
                dto.employerName(),
                dto.employmentSector(),
                dto.employmentStatus(),
                scale2(dto.basicSalary()),
                scale2(dto.totalSalary()),
                dto.employmentStartDate(),
                // AML
                dto.amlDeclarationCompleted(),
                dto.amlDeclarationAt(),
                // Bank account
                dto.disbursementBankCode(),
                dto.disbursementBankName(),
                dto.disbursementIban(),
                dto.disbursementAccountHolder(),
                dto.ibanVerified(),
                // Eligibility
                dto.simahConsent(),
                dto.simahConsentAt(),
                dto.creditScore(),
                dto.simahReferenceId(),
                scale2(dto.verifiedSalary()),
                scale2(dto.maxEligibleAmount()),
                // Offer
                scale2(dto.offeredAmount()),
                scale2(dto.installmentAmount() != null ? dto.installmentAmount() : dto.offeredMonthlyInstallment()),
                scale2(displayedTotalProfit),
                scale2(finalTotalPayable),
                scale2(dto.processingFee()),
                scale2(dto.adminFee()),
                scale2(dto.acceptedAmount()),
                // Contract
                dto.contractExpiresAt(),
                dto.otpVerified(),
                dto.otpAttempts(),
                dto.ivrVerified(),
                dto.ivrAttempts(),
                // NABA + PaymentGuard
                dto.nabaNotificationSent(),
                dto.paymentGuardStatus(),
                // Audit
                dto.createdAt(),
                dto.updatedAt(),
                // Loan / Disbursement
                dto.loanId(),
                dto.loanNumber(),
                dto.loanStatus(),
                scale2(dto.principalAmount()),
                scale2(finalTotalPayable),
                scale2(finalTotalPayable),
                scale2(dto.installmentAmount()),
                dto.fineractLoanId(),
                dto.disbursementDate(),
                // Current schedule (updated after reschedule)
                dto.currentTenureMonths(),
                dto.currentMaturityDate(),
                dto.currentProfitRate() != null ? normalizeProfitRate(dto.currentProfitRate()) : null,
                rescheduleId,
                rescheduleStatus,
                rescheduleType,
                rescheduleRequestedAt,
                rescheduleAppliedAt,
                rescheduleReason,
                rescheduleDetails,
                dto.earlySettlementEligible()
        );
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private static String deriveDisplayStatus(LoanApplicationDto dto, String normalizedStatus) {
        // If loan is already disbursed/active, list view should show DISBURSED even when app status lags.
        if (dto.disbursementDate() != null || isActiveOrClosedLoan(dto.loanStatus())) {
            return "DISBURSED";
        }
        return switch (normalizedStatus) {
            case "MANUAL_REVIEW" -> "MANUAL_REVIEW";
            case "APPROVED" -> "APPROVED";
            case "AWAIT_DISBURSED" -> "AWAIT_DISBURSED";
            case "DISBURSED" -> "DISBURSED";
            case "REJECTED" -> "REJECTED";
            case "CANCELLED" -> "CANCELLED";
            case "EXPIRED" -> "EXPIRED";
            default -> "IN_PROGRESS";
        };
    }

    private static boolean isActiveOrClosedLoan(String loanStatus) {
        if (loanStatus == null) {
            return false;
        }
        var normalizedLoanStatus = loanStatus.trim().toUpperCase(Locale.ROOT);
        return "ACTIVE".equals(normalizedLoanStatus)
                || "CLOSED".equals(normalizedLoanStatus)
                || "CLOSED_OBLIGATIONS_MET".equals(normalizedLoanStatus)
                || "OVERPAID".equals(normalizedLoanStatus);
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : null;
    }

    private static BigDecimal normalizeProfitRate(BigDecimal rate) {
        if (rate == null) return null;
        // If it's a decimal (e.g. 0.025), convert to percentage (2.5)
        // If it's already a percentage (e.g. 2.5), keep it
        if (rate.compareTo(BigDecimal.ONE) < 0) {
            return rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
        return rate.setScale(2, RoundingMode.HALF_UP);
    }
}
