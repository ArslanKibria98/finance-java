package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Pure domain service for BRD V1.8 finance calculation.
 * Zero framework imports — uses only JDK types.
 *
 * <p>BRD Formula (Page 14, Step 34):
 * <ul>
 *   <li>Total Cost of Financing = principal × profitRate × (tenureMonths / 12)</li>
 *   <li>Cost of Term = principal × costOfTermPercent (if separate; else same as total cost)</li>
 *   <li>Total Payable = principal + totalCostOfFinancing + processingFee + adminFee</li>
 *   <li>Monthly Installment = totalPayable / tenureMonths</li>
 *   <li>First Installment Due Date = today + 30 days</li>
 *   <li>APR = annualized cost rate (including profit and fees)</li>
 * </ul>
 */
public final class FinanceCalculationService {

    private static final int SCALE = 6;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private FinanceCalculationService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculate finance details per BRD V1.8 formula.
     *
     * @param principal         Financing amount (SAR)
     * @param profitRate        Annual profit rate as decimal (e.g., 0.0385 for 3.85%)
     * @param costOfTermPercent Cost-of-term percentage as decimal (e.g., 0.0385)
     *                          If null, defaults to profitRate (single-component mode)
     * @param tenureMonths      Loan tenure in months
     * @param processingFee     Processing fee amount (SAR), may be null/zero
     * @param adminFee          Administrative fee amount (SAR), may be null/zero
     * @return Complete calculation result
     */
    public static FinanceCalculationResult calculate(
            BigDecimal principal,
            BigDecimal profitRate,
            BigDecimal costOfTermPercent,
            int tenureMonths,
            BigDecimal processingFee,
            BigDecimal adminFee
    ) {
        validate(principal, profitRate, tenureMonths);

        var effectiveCostOfTermPercent = costOfTermPercent != null ? costOfTermPercent : profitRate;
        var safeProcFee = processingFee != null ? processingFee : BigDecimal.ZERO;
        var safeAdminFee = adminFee != null ? adminFee : BigDecimal.ZERO;

        // BRD: Total Cost of Financing = principal × profitRate × (tenureMonths / 12)
        var totalCostOfFinancing = principal
                .multiply(profitRate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(TWELVE, SCALE, RM);

        // BRD: Cost of Term = principal × costOfTermPercent × (tenureMonths / 12)
        var costOfTerm = principal
                .multiply(effectiveCostOfTermPercent)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(TWELVE, SCALE, RM);

        // BRD: Total Payable = principal + totalCostOfFinancing + processingFee + adminFee
        // (costOfTerm is the same breakdown view of totalCostOfFinancing in single-rate mode)
        var totalPayable = principal.add(totalCostOfFinancing)
                .add(safeProcFee)
                .add(safeAdminFee)
                .setScale(MONEY_SCALE, RM);

        // BRD: Monthly Installment = totalPayable / tenureMonths
        var monthlyInstallment = totalPayable
                .divide(BigDecimal.valueOf(tenureMonths), MONEY_SCALE, RM);

        // BRD: First Installment Due Date = today + 30 days
        var firstInstallmentDueDate = LocalDate.now().plusDays(30);

        // APR = ((totalCostOfFinancing + fees) / principal) / (tenureMonths / 12) × 100
        var totalCostWithFees = totalCostOfFinancing.add(safeProcFee).add(safeAdminFee);
        var apr = calculateApr(totalCostWithFees, principal, tenureMonths);

        return new FinanceCalculationResult(
                principal.setScale(MONEY_SCALE, RM),
                tenureMonths,
                tenureMonths,
                monthlyInstallment,
                totalCostOfFinancing.setScale(MONEY_SCALE, RM),
                costOfTerm.setScale(MONEY_SCALE, RM),
                totalPayable,
                safeProcFee.setScale(MONEY_SCALE, RM),
                safeAdminFee.setScale(MONEY_SCALE, RM),
                profitRate,
                apr,
                firstInstallmentDueDate
        );
    }

    /**
     * Calculate APR (Annual Percentage Rate) for disclosure.
     */
    public static BigDecimal calculateApr(
            BigDecimal totalCost,
            BigDecimal principal,
            int tenureMonths
    ) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }
        var tenureYears = BigDecimal.valueOf(tenureMonths).divide(TWELVE, SCALE, RM);
        if (tenureYears.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost
                .divide(principal, SCALE, RM)
                .divide(tenureYears, SCALE, RM)
                .multiply(HUNDRED)
                .setScale(MONEY_SCALE, RM);
    }

    /**
     * Calculate maximum eligible amount given DBR constraint.
     *
     * @param salary              Verified monthly salary
     * @param existingObligations Existing monthly obligations
     * @param maxDbrPercent       Maximum allowed DBR (e.g., 65)
     * @param profitRate          Annual profit rate
     * @param tenureMonths        Tenure in months
     * @return Maximum principal that keeps DBR within limit
     */
    public static BigDecimal calculateMaxEligibleAmount(
            BigDecimal salary,
            BigDecimal existingObligations,
            BigDecimal maxDbrPercent,
            BigDecimal profitRate,
            int tenureMonths
    ) {
        if (salary.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }

        // Available monthly capacity = salary × maxDBR% - existing obligations
        var availableForInstallment = salary
                .multiply(maxDbrPercent)
                .divide(HUNDRED, SCALE, RM)
                .subtract(existingObligations);

        if (availableForInstallment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Reverse: principal = availableInstallment × tenureMonths / (1 + rate × months/12)
        var rateFactor = BigDecimal.ONE.add(
                profitRate.multiply(BigDecimal.valueOf(tenureMonths)).divide(TWELVE, SCALE, RM)
        );

        return availableForInstallment
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(rateFactor, MONEY_SCALE, RM);
    }

    /**
     * Reverse-calculate max loan amount from a given max affordable installment.
     * principal = maxInstallment × tenureMonths / (1 + profitRate × months/12)
     */
    public static BigDecimal calculateMaxAmountFromInstallment(
            BigDecimal maxInstallment,
            BigDecimal profitRate,
            int tenureMonths
    ) {
        if (maxInstallment == null || maxInstallment.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }
        var rateFactor = BigDecimal.ONE.add(
                profitRate.multiply(BigDecimal.valueOf(tenureMonths)).divide(TWELVE, SCALE, RM)
        );
        return maxInstallment
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(rateFactor, MONEY_SCALE, RM);
    }

    private static void validate(BigDecimal principal, BigDecimal profitRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        if (profitRate == null || profitRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Profit rate must be non-negative");
        }
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure months must be positive");
        }
    }
}
