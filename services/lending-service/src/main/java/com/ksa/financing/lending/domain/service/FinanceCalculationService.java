package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Pure domain service implementing the reducing-balance (amortized) repayment engine
 * defined in {@code docs/reducing-balance-load-documentation.md}. Zero framework imports.
 *
 * <p>Formula:
 * <pre>
 *   r            = annualRate / 12
 *   EMI          = P × [ r(1+r)^n ] / [ (1+r)^n − 1 ]    (annuity)
 *   profit[i]    = openingBalance[i] × r                  (monthly profit on outstanding)
 *   principal[i] = EMI − profit[i]
 *   closing[i]   = opening[i] − principal[i]
 * </pre>
 *
 * <p>Special cases:
 * <ul>
 *   <li>r = 0 → EMI = P / n, all profit components are zero</li>
 *   <li>Last installment absorbs rounding remainder so closing[n] = 0 exactly</li>
 *   <li>Fees: inclusive → deducted at disbursement, not added to EMI base.
 *       Non-inclusive → fees distributed evenly as a separate per-installment fee component
 *       on top of the principal+profit EMI.</li>
 *   <li>VAT applied to total profit (per doc §12); VAT amount is included inside totalPayable.</li>
 * </ul>
 */
public final class FinanceCalculationService {

    private static final int SCALE = 10;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_VAT_PERCENTAGE = new BigDecimal("15");

    private FinanceCalculationService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculate loan finance details using reducing-balance methodology.
     *
     * @param principal               Financing amount (SAR)
     * @param annualRate              Annual profit rate as decimal (e.g., 0.05 for 5%)
     * @param tenureMonths            Loan tenure in months
     * @param processingFee           Processing fee amount (SAR), may be null/zero
     * @param adminFee                Administrative fee amount (SAR), may be null/zero
     * @param vatPercentage           VAT as percent (e.g., 15 for 15%), null defaults to 15
     * @param isDisbursementInclusive TRUE → fees deducted from disbursement (not added to repayment).
     *                                FALSE → fees added evenly to repayment schedule.
     */
    public static FinanceCalculationResult calculate(
            BigDecimal principal,
            BigDecimal annualRate,
            int tenureMonths,
            BigDecimal processingFee,
            BigDecimal adminFee,
            BigDecimal vatPercentage,
            boolean isDisbursementInclusive
    ) {
        validate(principal, annualRate, tenureMonths);

        var safeProcFee = processingFee != null ? processingFee : BigDecimal.ZERO;
        var safeAdminFee = adminFee != null ? adminFee : BigDecimal.ZERO;
        var totalFees = safeProcFee.add(safeAdminFee);
        var hasFees = totalFees.compareTo(BigDecimal.ZERO) > 0;
        var safeVatPercentage = vatPercentage != null ? vatPercentage : DEFAULT_VAT_PERCENTAGE;

        // Monthly profit rate: r = annualRate / 12
        var monthlyRate = annualRate.divide(TWELVE, SCALE, RM);

        // EMI on principal only (reducing balance annuity)
        var emiPrincipalProfit = annuityEmi(principal, monthlyRate, tenureMonths);

        // Walk the schedule to compute total profit (sum of monthly profits on declining balance)
        var totalProfit = sumProfit(principal, monthlyRate, emiPrincipalProfit, tenureMonths);

        // Fee component added to each installment when non-inclusive
        var feePerInstallment = (!isDisbursementInclusive && hasFees)
                ? totalFees.divide(BigDecimal.valueOf(tenureMonths), MONEY_SCALE, RM)
                : BigDecimal.ZERO;

        var monthlyInstallment = emiPrincipalProfit.add(feePerInstallment).setScale(MONEY_SCALE, RM);

        // VAT on profit only (doc §12). VAT is included in totalPayable.
        var vatOnProfit = totalProfit
                .multiply(safeVatPercentage)
                .divide(HUNDRED, SCALE, RM)
                .setScale(MONEY_SCALE, RM);
        var totalProfitBeforeVat = totalProfit.setScale(MONEY_SCALE, RM);
        var totalProfitAfterVat = totalProfitBeforeVat.subtract(vatOnProfit);

        // Total payable: principal + profit + (fees only when non-inclusive)
        var totalPayable = principal.add(totalProfitBeforeVat);
        if (!isDisbursementInclusive && hasFees) {
            totalPayable = totalPayable.add(totalFees);
        }
        totalPayable = totalPayable.setScale(MONEY_SCALE, RM);

        var firstInstallmentDueDate = LocalDate.now().plusDays(30);

        // APR (for disclosure)
        var apr = calculateApr(totalProfitBeforeVat, principal, tenureMonths);

        return new FinanceCalculationResult(
                principal.setScale(MONEY_SCALE, RM),
                tenureMonths,
                tenureMonths,
                monthlyInstallment,
                totalProfitBeforeVat,
                totalProfitBeforeVat,
                totalPayable,
                safeProcFee.setScale(MONEY_SCALE, RM),
                safeAdminFee.setScale(MONEY_SCALE, RM),
                annualRate,
                apr,
                firstInstallmentDueDate,
                vatOnProfit,
                totalProfitBeforeVat,
                totalProfitAfterVat,
                isDisbursementInclusive,
                safeVatPercentage,
                java.util.List.of()
        );
    }

    /** Backward-compatible overload — defaults VAT=15 and isDisbursementInclusive=TRUE. */
    public static FinanceCalculationResult calculate(
            BigDecimal principal,
            BigDecimal annualRate,
            BigDecimal unusedCostOfTermPercent,
            int tenureMonths,
            BigDecimal processingFee,
            BigDecimal adminFee
    ) {
        return calculate(principal, annualRate, tenureMonths, processingFee, adminFee,
                DEFAULT_VAT_PERCENTAGE, true);
    }

    /**
     * Annuity EMI formula:
     *   EMI = P × r(1+r)^n / ((1+r)^n − 1)
     * Falls back to P/n when r == 0.
     */
    public static BigDecimal annuityEmi(BigDecimal principal, BigDecimal monthlyRate, int n) {
        if (n <= 0) {
            return BigDecimal.ZERO;
        }
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), MONEY_SCALE, RM);
        }
        var onePlusR = BigDecimal.ONE.add(monthlyRate);
        var onePlusRPowN = onePlusR.pow(n);
        var numerator = monthlyRate.multiply(onePlusRPowN);
        var denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        return principal.multiply(numerator).divide(denominator, MONEY_SCALE, RM);
    }

    /**
     * Walk the reducing-balance schedule and sum monthly profit.
     * Final installment absorbs the principal remainder, so total profit reflects rounding.
     */
    private static BigDecimal sumProfit(BigDecimal principal, BigDecimal monthlyRate,
                                        BigDecimal emi, int n) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        var outstanding = principal;
        var totalProfit = BigDecimal.ZERO;
        for (int i = 1; i <= n; i++) {
            var profit = outstanding.multiply(monthlyRate).setScale(MONEY_SCALE, RM);
            BigDecimal principalPortion;
            if (i == n) {
                principalPortion = outstanding;
            } else {
                principalPortion = emi.subtract(profit);
            }
            outstanding = outstanding.subtract(principalPortion);
            totalProfit = totalProfit.add(profit);
        }
        return totalProfit;
    }

    /** APR for disclosure: (total cost / principal) / years × 100 */
    public static BigDecimal calculateApr(BigDecimal totalCost, BigDecimal principal, int tenureMonths) {
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
     * Max eligible principal given an affordability ceiling on EMI under reducing balance.
     * Inverts the annuity formula: P = EMI × ((1+r)^n − 1) / (r(1+r)^n)
     */
    public static BigDecimal calculateMaxEligibleAmount(
            BigDecimal salary,
            BigDecimal existingObligations,
            BigDecimal maxDbrPercent,
            BigDecimal annualRate,
            int tenureMonths
    ) {
        if (salary.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }
        var availableForInstallment = salary
                .multiply(maxDbrPercent)
                .divide(HUNDRED, SCALE, RM)
                .subtract(existingObligations);
        if (availableForInstallment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return calculateMaxAmountFromInstallment(availableForInstallment, annualRate, tenureMonths);
    }

    /** Reverse-calculate max principal from a given max affordable EMI under reducing balance. */
    public static BigDecimal calculateMaxAmountFromInstallment(
            BigDecimal maxInstallment,
            BigDecimal annualRate,
            int tenureMonths
    ) {
        if (maxInstallment == null || maxInstallment.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }
        var monthlyRate = annualRate.divide(TWELVE, SCALE, RM);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return maxInstallment.multiply(BigDecimal.valueOf(tenureMonths)).setScale(MONEY_SCALE, RM);
        }
        var onePlusRPowN = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths);
        var numerator = onePlusRPowN.subtract(BigDecimal.ONE);
        var denominator = monthlyRate.multiply(onePlusRPowN);
        return maxInstallment.multiply(numerator).divide(denominator, MONEY_SCALE, RM);
    }

    private static void validate(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual rate must be non-negative");
        }
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure months must be positive");
        }
    }
}
