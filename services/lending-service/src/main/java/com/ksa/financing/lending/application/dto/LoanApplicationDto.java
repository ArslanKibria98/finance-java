package com.ksa.financing.lending.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for LoanApplicationAggregate. Matches the UI stepper data model.
 */
public record LoanApplicationDto(
        String id,
        String tenantId,
        String applicationNumber,
        String customerId,
        String nationalId,
        String status,
        int stepperIndex,
        String stepperLabel,
        String workflowId,

        // Pre-qualification
        BigDecimal monthlyIncome,
        BigDecimal totalExpenses,
        BigDecimal existingLiabilities,
        int adultDependents,
        int childDependents,

        // Individual expense categories
        BigDecimal foodGroceries,
        BigDecimal utilities,
        BigDecimal healthcare,
        BigDecimal communication,
        BigDecimal housingRent,
        BigDecimal clothingEssentials,
        BigDecimal education,
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
        BigDecimal profitRate,
        BigDecimal apr,

        // SafeWatch AML
        String safeWatchSessionId,
        String safeWatchStatus,

        // Masdar Employment
        String employerName,
        String employmentSector,
        String employmentStatus,
        BigDecimal basicSalary,
        BigDecimal totalSalary,
        String employmentStartDate,

        // AML Declaration
        boolean amlDeclarationCompleted,
        LocalDateTime amlDeclarationAt,

        // Step 2: Bank Account
        String disbursementBankCode,
        String disbursementBankName,
        String disbursementIban,
        String disbursementAccountHolder,
        boolean ibanVerified,

        // Step 3: Eligibility
        boolean simahConsent,
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

        // Step 5: Contract
        LocalDateTime contractExpiresAt,
        boolean otpVerified,
        int otpAttempts,
        boolean ivrVerified,
        int ivrAttempts,

        // NABA + PaymentGuard
        boolean nabaNotificationSent,
        String paymentGuardSessionId,
        String paymentGuardStatus,

        // Audit
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version,

        // Loan / Disbursement (joined from loans table)
        String loanId,
        String loanNumber,
        String loanStatus,
        BigDecimal principalAmount,
        BigDecimal totalAmount,
        BigDecimal installmentAmount,
        String fineractLoanId,
        java.time.LocalDate disbursementDate,

        // Current schedule (updated after reschedule — differs from requestedTenureMonths)
        Integer currentTenureMonths,
        java.time.LocalDate currentMaturityDate,
        BigDecimal currentProfitRate,

        @io.swagger.v3.oas.annotations.media.Schema(description = "True if the loan currently qualifies for early settlement (sourced from collections-service)")
        Boolean earlySettlementEligible
) {}
