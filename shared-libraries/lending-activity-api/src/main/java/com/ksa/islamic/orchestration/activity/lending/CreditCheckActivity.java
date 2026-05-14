package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Performs credit check by calling risk-service (which calls SIMAH via middleware).
 * Used in Step 3 (Checking Eligibility) of the loan application workflow.
 */
@ActivityInterface
public interface CreditCheckActivity {

    @ActivityMethod
    CreditCheckResult performCreditCheck(CreditCheckInput input);

    @ActivityMethod
    EligibilityResult calculateEligibility(EligibilityInput input);

    @ActivityMethod
    ProfitCalculationResult calculateOffer(ProfitCalculationInput input);

    record CreditCheckInput(
            String tenantId,
            String nationalId,
            String customerId,
            String applicationId,
            BigDecimal requestedAmount
    ) {}

    record CreditCheckResult(
            int creditScore,
            String simahGrade,
            String simahReferenceId,
            BigDecimal verifiedSalary,
            BigDecimal existingObligations,
            boolean hasActiveDefaults
    ) {}

    record EligibilityInput(
            String tenantId,
            // Credit data
            int creditScore,
            BigDecimal verifiedSalary,
            BigDecimal existingObligations,
            boolean hasActiveDefaults,
            // Customer data
            int customerAge,
            int employmentDurationMonths,
            // Product criteria
            int minCreditScore,
            BigDecimal minSalary,
            int minAge,
            int maxAge,
            int minEmploymentMonths,
            BigDecimal maxDbrPercent,
            // Requested
            BigDecimal requestedAmount,
            BigDecimal profitRate,
            int requestedTenureMonths,
            // BRD Affordability: customer-declared expenses (sum of 8 categories)
            BigDecimal declaredMonthlyIncome,
            BigDecimal declaredExpenses,
            BigDecimal declaredLiabilities
    ) {}

    record EligibilityResult(
            boolean eligible,
            BigDecimal maxEligibleAmount,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            BigDecimal disposableIncome,
            String rejectionReason
    ) {}

    record ProfitCalculationInput(
            String shariaStructure,
            BigDecimal principalAmount,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal processingFeePercent,
            BigDecimal processingFeeAmount,
            BigDecimal adminFeeAmount
    ) {}

    record ProfitCalculationResult(
            BigDecimal monthlyInstallment,
            BigDecimal totalProfit,
            BigDecimal totalPayable,
            BigDecimal sellingPrice,
            BigDecimal processingFee,
            BigDecimal adminFee,
            BigDecimal apr,
            java.time.LocalDate firstInstallmentDueDate
    ) {}
}
