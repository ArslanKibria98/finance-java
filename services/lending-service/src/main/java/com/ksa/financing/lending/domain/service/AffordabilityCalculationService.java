package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure domain service for affordability / DBR calculation.
 * Zero framework imports.
 *
 * <p>BRD Steps 4-10: Pre-qualification check with expenses and DBR.
 */
public final class AffordabilityCalculationService {

    private static final int SCALE = 6;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private AffordabilityCalculationService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Check affordability based on salary, expenses, liabilities, and proposed installment.
     *
     * @param salary              Monthly verified salary
     * @param liabilities         Existing monthly obligations/liabilities
     * @param totalExpenses       Sum of all 8 expense categories
     * @param proposedInstallment Proposed monthly installment for new finance
     * @param maxDbrPercent       Maximum allowed DBR (e.g., 65)
     * @return Affordability result with eligible flag and DBR values
     */
    public static AffordabilityResult check(
            BigDecimal salary,
            BigDecimal liabilities,
            BigDecimal totalExpenses,
            BigDecimal proposedInstallment,
            BigDecimal maxDbrPercent
    ) {
        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            return new AffordabilityResult(false, BigDecimal.ZERO, BigDecimal.valueOf(100),
                    BigDecimal.ZERO, "Salary must be positive");
        }

        var safeLiabilities = liabilities != null ? liabilities : BigDecimal.ZERO;
        var safeExpenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;

        // DBR before new finance = existing liabilities / salary × 100
        var dbrBefore = safeLiabilities
                .divide(salary, SCALE, RM)
                .multiply(HUNDRED)
                .setScale(2, RM);

        // DBR after new finance = (existing liabilities + proposed installment) / salary × 100
        var totalObligations = safeLiabilities.add(proposedInstallment);
        var dbrAfter = totalObligations
                .divide(salary, SCALE, RM)
                .multiply(HUNDRED)
                .setScale(2, RM);

        // Disposable income check
        var disposableIncome = salary.subtract(safeExpenses).subtract(safeLiabilities).subtract(proposedInstallment);

        var effectiveMaxDbr = maxDbrPercent != null ? maxDbrPercent : BigDecimal.valueOf(65);

        if (dbrAfter.compareTo(effectiveMaxDbr) > 0) {
            return new AffordabilityResult(false, dbrBefore, dbrAfter, disposableIncome,
                    "DBR " + dbrAfter + "% exceeds maximum " + effectiveMaxDbr + "%");
        }

        if (disposableIncome.compareTo(BigDecimal.ZERO) < 0) {
            return new AffordabilityResult(false, dbrBefore, dbrAfter, disposableIncome,
                    "Insufficient disposable income after expenses and installment");
        }

        return new AffordabilityResult(true, dbrBefore, dbrAfter, disposableIncome, null);
    }

    /**
     * Sum up 8 BRD expense categories.
     */
    public static BigDecimal sumExpenses(
            BigDecimal foodGroceries, BigDecimal utilities, BigDecimal healthcare,
            BigDecimal communication, BigDecimal housingRent, BigDecimal clothingEssentials,
            BigDecimal education, BigDecimal transportation
    ) {
        var total = BigDecimal.ZERO;
        if (foodGroceries != null) total = total.add(foodGroceries);
        if (utilities != null) total = total.add(utilities);
        if (healthcare != null) total = total.add(healthcare);
        if (communication != null) total = total.add(communication);
        if (housingRent != null) total = total.add(housingRent);
        if (clothingEssentials != null) total = total.add(clothingEssentials);
        if (education != null) total = total.add(education);
        if (transportation != null) total = total.add(transportation);
        return total;
    }

    public record AffordabilityResult(
            boolean eligible,
            BigDecimal dbrBefore,
            BigDecimal dbrAfter,
            BigDecimal disposableIncome,
            String reason
    ) {}
}
