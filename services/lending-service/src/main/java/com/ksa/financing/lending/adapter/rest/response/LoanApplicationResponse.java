package com.ksa.financing.lending.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ksa.financing.lending.application.dto.LoanApplicationDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Schema(description = "Loan application response with stepper state")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoanApplicationResponse(
        String id,
        String applicationNumber,
        String customerId,
        String nationalId,
        String status,
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
        LocalDateTime updatedAt
) {
    public static LoanApplicationResponse from(LoanApplicationDto dto) {
        return new LoanApplicationResponse(
                dto.id(),
                dto.applicationNumber(),
                dto.customerId(),
                dto.nationalId(),
                dto.status(),
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
                dto.requestedTenureMonths(),
                dto.purposeOfFinance(),
                dto.purposeOfFinanceOther(),
                dto.profitRate(),
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
                scale2(dto.offeredMonthlyInstallment()),
                scale2(dto.offeredTotalProfit()),
                scale2(dto.offeredTotalPayable()),
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
                dto.updatedAt()
        );
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : null;
    }
}
