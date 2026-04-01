package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure domain service for affordability / DBR calculation.
 * Zero framework imports.
 *
 * <p>Implements dual-check model from BRD affordability matrix:
 * <ol>
 *   <li>DTI capacity = income × maxDTI − existingDebts</li>
 *   <li>Residual capacity = (income − debts − minExpenses) × (1 − stressBuffer) × affordabilityFactor</li>
 *   <li>Final max affordable = MIN(DTI, Residual) — the binding constraint</li>
 * </ol>
 */
public final class AffordabilityCalculationService {

    private static final int SCALE = 6;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // ── Policy parameters (from Config sheet) ──
    private static final BigDecimal DEFAULT_MAX_DTI = new BigDecimal("0.33");
    private static final BigDecimal DEFAULT_STRESS_BUFFER = new BigDecimal("0.03");
    private static final BigDecimal DEFAULT_AFFORDABILITY_FACTOR = BigDecimal.ONE;

    // ── BRD minimum expense thresholds per base adult (SAR) ──
    // Source: Expense Matrix sheet (gid=1332845377) — Base_Adult column
    private static final BigDecimal MIN_FOOD_GROCERIES = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_UTILITIES = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_TRANSPORTATION = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_HEALTHCARE = BigDecimal.valueOf(100);
    private static final BigDecimal MIN_COMMUNICATION = BigDecimal.valueOf(50);
    private static final BigDecimal MIN_HOUSING_RENT = BigDecimal.valueOf(200);
    private static final BigDecimal MIN_EDUCATION = BigDecimal.valueOf(50);
    private static final BigDecimal MIN_CLOTHING_ESSENTIALS = BigDecimal.valueOf(100);
    // Total base adult minimum = 800 SAR

    // ── Per-dependent overhead (from expense matrix sheet) ──
    private static final BigDecimal ADDITIONAL_ADULT_EXPENSE = BigDecimal.valueOf(800);
    private static final BigDecimal CHILD_EXPENSE = BigDecimal.valueOf(700);

    // ── Region cost multipliers ──
    private static final java.util.Map<String, BigDecimal> REGION_MULTIPLIERS = java.util.Map.ofEntries(
            java.util.Map.entry("RIYADH", new BigDecimal("1.00")),
            java.util.Map.entry("JEDDAH", new BigDecimal("1.02")),
            java.util.Map.entry("DAMMAM", new BigDecimal("0.98")),
            java.util.Map.entry("KHOBAR", new BigDecimal("0.98")),
            java.util.Map.entry("MAKKAH", new BigDecimal("1.00")),
            java.util.Map.entry("MADINAH", new BigDecimal("0.98")),
            java.util.Map.entry("ABHA", new BigDecimal("0.95")),
            java.util.Map.entry("TABUK", new BigDecimal("0.94")),
            java.util.Map.entry("JAZAN", new BigDecimal("0.92")),
            java.util.Map.entry("HAIL", new BigDecimal("0.93")),
            java.util.Map.entry("NAJRAN", new BigDecimal("0.92"))
    );

    // ── Income bracket multipliers ──
    private static final int[][] INCOME_BRACKETS = {
            {0, 85}, {5000, 90}, {10000, 95}, {15000, 100}, {20000, 100}, {25000, 100}, {30000, 105}
    };

    private AffordabilityCalculationService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Full affordability check with dual constraint (DTI + Residual).
     *
     * @param salary              Monthly verified salary
     * @param liabilities         Existing monthly obligations (SIMAH)
     * @param totalExpenses       Sum of all 8 expense categories (with minimums enforced)
     * @param proposedInstallment Proposed monthly installment for new finance
     * @param maxDbrPercent       Product-level max DBR override (percent, e.g. 33). Null = use default 33%
     * @return Affordability result with eligible flag and breakdown
     */
    public static AffordabilityResult check(
            BigDecimal salary,
            BigDecimal liabilities,
            BigDecimal totalExpenses,
            BigDecimal proposedInstallment,
            BigDecimal maxDbrPercent
    ) {
        return check(salary, liabilities, totalExpenses, proposedInstallment, maxDbrPercent, null);
    }

    /**
     * Full affordability check with region adjustment.
     */
    public static AffordabilityResult check(
            BigDecimal salary,
            BigDecimal liabilities,
            BigDecimal totalExpenses,
            BigDecimal proposedInstallment,
            BigDecimal maxDbrPercent,
            String region
    ) {
        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            return new AffordabilityResult(false, BigDecimal.ZERO, BigDecimal.valueOf(100),
                    BigDecimal.ZERO, BigDecimal.ZERO, "Salary must be positive");
        }

        var safeLiabilities = liabilities != null ? liabilities : BigDecimal.ZERO;
        var safeExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;

        // Apply region cost multiplier to expenses
        var regionMultiplier = getRegionMultiplier(region);
        var adjustedExpenses = safeExpenses.multiply(regionMultiplier).setScale(2, RM);

        // Apply income bracket multiplier
        var incomeMultiplier = getIncomeMultiplier(salary);

        // Effective max DTI (convert from percent if needed, or use default 0.33)
        var maxDti = DEFAULT_MAX_DTI;
        if (maxDbrPercent != null && maxDbrPercent.compareTo(BigDecimal.ZERO) > 0) {
            // If passed as percent (e.g. 33 or 65), convert to decimal
            maxDti = maxDbrPercent.compareTo(BigDecimal.ONE) > 0
                    ? maxDbrPercent.divide(HUNDRED, SCALE, RM)
                    : maxDbrPercent;
        }

        // ── CHECK 1: DTI capacity ──
        // Max instalment by DTI = salary × maxDTI − existingDebts
        var dtiCapacity = salary.multiply(maxDti).subtract(safeLiabilities).setScale(2, RM);

        // ── CHECK 2: Residual capacity ──
        // Residual income = salary − debts − adjustedExpenses
        var residualIncome = salary.subtract(safeLiabilities).subtract(adjustedExpenses);

        // Available installment = residual × incomeMultiplier × (1 − stressBuffer) × affordabilityFactor
        var oneMinusStress = BigDecimal.ONE.subtract(DEFAULT_STRESS_BUFFER);
        var residualCapacity = residualIncome.compareTo(BigDecimal.ZERO) > 0
                ? residualIncome.multiply(incomeMultiplier).multiply(oneMinusStress).multiply(DEFAULT_AFFORDABILITY_FACTOR).setScale(2, RM)
                : BigDecimal.ZERO;

        // ── Final max affordable = MIN(DTI capacity, Residual capacity) ──
        var finalMaxAffordable = dtiCapacity.min(residualCapacity);
        if (finalMaxAffordable.compareTo(BigDecimal.ZERO) < 0) {
            finalMaxAffordable = BigDecimal.ZERO;
        }

        // ── DBR values for reporting ──
        var dbrBefore = safeLiabilities
                .divide(salary, SCALE, RM)
                .multiply(HUNDRED)
                .setScale(2, RM);

        var dbrAfter = safeLiabilities.add(proposedInstallment)
                .divide(salary, SCALE, RM)
                .multiply(HUNDRED)
                .setScale(2, RM);

        var disposableIncome = salary.subtract(adjustedExpenses).subtract(safeLiabilities).subtract(proposedInstallment);

        // ── Pass/Fail ──
        if (proposedInstallment.compareTo(finalMaxAffordable) > 0) {
            var reasons = new java.util.ArrayList<String>();
            if (proposedInstallment.compareTo(dtiCapacity) > 0) {
                reasons.add("DTI exceeded: instalment " + proposedInstallment + " SAR > DTI capacity " + dtiCapacity + " SAR");
            }
            if (proposedInstallment.compareTo(residualCapacity) > 0) {
                reasons.add("Insufficient residual: instalment " + proposedInstallment + " SAR > residual capacity " + residualCapacity + " SAR");
            }
            return new AffordabilityResult(false, dbrBefore, dbrAfter, disposableIncome,
                    finalMaxAffordable, String.join("; ", reasons));
        }

        return new AffordabilityResult(true, dbrBefore, dbrAfter, disposableIncome, finalMaxAffordable, null);
    }

    /**
     * Sum up 8 BRD expense categories with minimum threshold enforcement.
     * Enforces BRD minimums — customer cannot declare expenses below floor.
     */
    public static BigDecimal sumExpenses(
            BigDecimal foodGroceries, BigDecimal utilities, BigDecimal healthcare,
            BigDecimal communication, BigDecimal housingRent, BigDecimal clothingEssentials,
            BigDecimal education, BigDecimal transportation
    ) {
        return sumExpenses(foodGroceries, utilities, healthcare, communication,
                housingRent, clothingEssentials, education, transportation, 0, 0);
    }

    /**
     * Sum expenses with dependents factor.
     * Base adult minimum = 1,500 SAR. Each additional adult +800, each child +700.
     */
    public static BigDecimal sumExpenses(
            BigDecimal foodGroceries, BigDecimal utilities, BigDecimal healthcare,
            BigDecimal communication, BigDecimal housingRent, BigDecimal clothingEssentials,
            BigDecimal education, BigDecimal transportation,
            int additionalAdults, int numberOfChildren
    ) {
        var total = BigDecimal.ZERO;
        total = total.add(enforceMin(foodGroceries, MIN_FOOD_GROCERIES));
        total = total.add(enforceMin(utilities, MIN_UTILITIES));
        total = total.add(enforceMin(healthcare, MIN_HEALTHCARE));
        total = total.add(enforceMin(communication, MIN_COMMUNICATION));
        total = total.add(enforceMin(housingRent, MIN_HOUSING_RENT));
        total = total.add(enforceMin(clothingEssentials, MIN_CLOTHING_ESSENTIALS));
        total = total.add(enforceMin(education, MIN_EDUCATION));
        total = total.add(enforceMin(transportation, MIN_TRANSPORTATION));

        if (additionalAdults > 0) {
            total = total.add(ADDITIONAL_ADULT_EXPENSE.multiply(BigDecimal.valueOf(additionalAdults)));
        }
        if (numberOfChildren > 0) {
            total = total.add(CHILD_EXPENSE.multiply(BigDecimal.valueOf(numberOfChildren)));
        }

        return total;
    }

    // ── Helpers ──

    private static BigDecimal enforceMin(BigDecimal value, BigDecimal minimum) {
        if (value == null || value.compareTo(minimum) < 0) {
            return minimum;
        }
        return value;
    }

    static BigDecimal getRegionMultiplier(String region) {
        if (region == null || region.isBlank()) return BigDecimal.ONE;
        return REGION_MULTIPLIERS.getOrDefault(region.toUpperCase().trim(), BigDecimal.ONE);
    }

    static BigDecimal getIncomeMultiplier(BigDecimal salary) {
        if (salary == null) return new BigDecimal("0.85");
        int salaryInt = salary.intValue();
        BigDecimal multiplier = new BigDecimal("0.85"); // default lowest bracket
        for (int[] bracket : INCOME_BRACKETS) {
            if (salaryInt >= bracket[0]) {
                multiplier = BigDecimal.valueOf(bracket[1]).divide(HUNDRED, 2, RM);
            }
        }
        return multiplier;
    }

    public record AffordabilityResult(
            boolean eligible,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            BigDecimal disposableIncome,
            BigDecimal maxAffordableInstalment,
            String reason
    ) {}
}
