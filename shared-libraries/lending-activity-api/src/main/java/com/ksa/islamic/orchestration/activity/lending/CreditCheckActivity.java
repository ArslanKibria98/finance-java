package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Performs credit check by calling risk-service (which calls SIMAH via middleware).
 * Used in Step 3 (Checking Eligibility) of the loan application workflow.
 */
@ActivityInterface
public interface CreditCheckActivity {

    @ActivityMethod
    CreditCheckResult performCreditCheck(CreditCheckInput input);

    /**
     * Runs the product-driven credit decision engine in risk-service (LOS §5 Step 4).
     * Returns a Green/Amber/Red decision with score breakdown.
     */
    @ActivityMethod
    CreditDecisionResult runCreditDecisionEngine(CreditDecisionInput input);

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
            boolean hasActiveDefaults,
            int defaultsCount,
            int activeLoansCount
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

    /**
     * Input for the product-driven credit decision engine call.
     *
     * @param answers   field_key → string value (e.g. {@code creditScore → "720"},
     *                  {@code verifiedSalary → "15000"}). Keys must match
     *                  {@code credit_scoring_field_definitions.field_key} rows in risk-service.
     * @param greenThreshold optional per-product override for green threshold (score %); null → service default
     * @param amberThreshold optional per-product override for amber threshold (score %); null → service default
     */
    record CreditDecisionInput(
            String tenantId,
            String productId,
            String applicationId,
            Map<String, String> answers,
            BigDecimal greenThreshold,
            BigDecimal amberThreshold
    ) {}

    record CriteriaDetail(
            String fieldKey,
            String fieldName,
            String customerValue,
            boolean passed,
            BigDecimal scoredWeight,
            BigDecimal maxWeight,
            String matchedRule,
            String failureReason
    ) {}

    /**
     * Result of the credit decision engine.
     *
     * @param decision one of {@code AUTO_APPROVE | REFER_MANUAL_REVIEW | AUTO_REJECT}.
     *                 String (not enum) because Temporal data converter is friendlier with primitives.
     * @param reasonCode short code ({@code GREEN_AUTO_APPROVE | AMBER_MANUAL_REVIEW | RED_AUTO_REJECT | NO_CRITERIA})
     */
    record CreditDecisionResult(
            String decision,
            String reasonCode,
            boolean eligible,
            BigDecimal totalScore,
            BigDecimal maxPossibleScore,
            BigDecimal scorePercentage,
            BigDecimal greenThreshold,
            BigDecimal amberThreshold,
            int totalCriteria,
            int matchedCriteria,
            int failedCriteria,
            List<CriteriaDetail> details,
            String summary
    ) {}
}
