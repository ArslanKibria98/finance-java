package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Pure domain service for KSA Islamic financing calculation (flat-rate Murabaha).
 * Zero framework imports — uses only JDK types.
 *
 * <p>Formula (4-case branch with VAT and IsDisbursementInclusive flag):
 * <ol>
 *   <li>profitFromPercentage = principal × profitRate × (tenureMonths / 12)  (annual rate)</li>
 *   <li>totalProfitBeforeVat is determined by 4 cases:
 *     <ul>
 *       <li><b>CASE 1</b> profitRate &lt; 0.01 → totalProfitBeforeVat = processingFee + adminFee</li>
 *       <li><b>CASE 2A</b> hasFees + isDisbursementInclusive=TRUE  → totalProfitBeforeVat = profitFromPercentage</li>
 *       <li><b>CASE 2B</b> hasFees + isDisbursementInclusive=FALSE → totalProfitBeforeVat = profitFromPercentage + fees</li>
 *       <li><b>CASE 2C</b> no fees → totalProfitBeforeVat = profitFromPercentage</li>
 *     </ul>
 *   </li>
 *   <li>vatOnProfit         = totalProfitBeforeVat × (vatPercentage / 100)</li>
 *   <li>totalProfitAfterVat = totalProfitBeforeVat − vatOnProfit</li>
 *   <li>totalPayable        = principal + totalProfitAfterVat + vatOnProfit</li>
 *   <li>monthlyInstallment  = totalPayable / tenureMonths</li>
 *   <li>firstInstallmentDueDate = today + 30 days</li>
 *   <li>APR = (totalCostWithFees / principal) / (tenureMonths / 12) × 100</li>
 * </ol>
 */
public final class FinanceCalculationService {

    private static final int SCALE = 6;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE_PERCENT_DECIMAL = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_VAT_PERCENTAGE = new BigDecimal("15");

    private FinanceCalculationService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculate finance details with full 4-case branch logic.
     *
     * @param principal               Financing amount (SAR)
     * @param profitRate              Annual profit rate as decimal (e.g., 0.05 for 5%)
     * @param tenureMonths            Loan tenure in months
     * @param processingFee           Processing fee amount (SAR), may be null/zero
     * @param adminFee                Administrative fee amount (SAR), may be null/zero
     * @param vatPercentage           VAT as percent (e.g., 15 for 15%), null defaults to 15
     * @param isDisbursementInclusive TRUE → fees deducted from disbursement (not added to repayment).
     *                                FALSE → fees added to repayment.
     * @return Complete calculation result
     */
    public static FinanceCalculationResult calculate(
            BigDecimal principal,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal processingFee,
            BigDecimal adminFee,
            BigDecimal vatPercentage,
            boolean isDisbursementInclusive
    ) {
        validate(principal, profitRate, tenureMonths);

        var safeProcFee = processingFee != null ? processingFee : BigDecimal.ZERO;
        var safeAdminFee = adminFee != null ? adminFee : BigDecimal.ZERO;
        var totalFees = safeProcFee.add(safeAdminFee);
        var hasFees = totalFees.compareTo(BigDecimal.ZERO) > 0;
        var safeVatPercentage = vatPercentage != null ? vatPercentage : DEFAULT_VAT_PERCENTAGE;

        // Step 1 — Annual profit from percentage
        var profitFromPercentage = principal
                .multiply(profitRate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(TWELVE, SCALE, RM);

        // Step 2 — Total profit before VAT (4 cases)
        BigDecimal totalProfitBeforeVat;
        if (profitRate.compareTo(ONE_PERCENT_DECIMAL) < 0) {
            // CASE 1: zero/sub-1% profit → fees become the only profit
            totalProfitBeforeVat = totalFees;
        } else if (hasFees && isDisbursementInclusive) {
            // CASE 2A: fees already deducted at disbursement
            totalProfitBeforeVat = profitFromPercentage;
        } else if (hasFees) {
            // CASE 2B: fees added to repayment
            totalProfitBeforeVat = profitFromPercentage.add(totalFees);
        } else {
            // CASE 2C: no fees
            totalProfitBeforeVat = profitFromPercentage;
        }

        // Step 3 — VAT on profit
        var vatOnProfit = totalProfitBeforeVat
                .multiply(safeVatPercentage)
                .divide(HUNDRED, SCALE, RM);

        // Step 4 — Profit after VAT
        var totalProfitAfterVat = totalProfitBeforeVat.subtract(vatOnProfit);

        // Step 5 — Total payable
        var totalPayable = principal
                .add(totalProfitAfterVat)
                .add(vatOnProfit)
                .setScale(MONEY_SCALE, RM);

        // Step 6 — Equal monthly installment
        var monthlyInstallment = totalPayable
                .divide(BigDecimal.valueOf(tenureMonths), MONEY_SCALE, RM);

        // Step 7 — First due date
        var firstInstallmentDueDate = LocalDate.now().plusDays(30);

        // APR (for disclosure)
        var apr = calculateApr(totalProfitBeforeVat, principal, tenureMonths);

        return new FinanceCalculationResult(
                principal.setScale(MONEY_SCALE, RM),
                tenureMonths,
                tenureMonths,
                monthlyInstallment,
                totalProfitBeforeVat.setScale(MONEY_SCALE, RM),
                profitFromPercentage.setScale(MONEY_SCALE, RM),
                totalPayable,
                safeProcFee.setScale(MONEY_SCALE, RM),
                safeAdminFee.setScale(MONEY_SCALE, RM),
                profitRate,
                apr,
                firstInstallmentDueDate,
                vatOnProfit.setScale(MONEY_SCALE, RM),
                totalProfitBeforeVat.setScale(MONEY_SCALE, RM),
                totalProfitAfterVat.setScale(MONEY_SCALE, RM),
                isDisbursementInclusive,
                safeVatPercentage,
                java.util.List.of()
        );
    }

    /**
     * Backward-compatible overload — defaults VAT=15 and isDisbursementInclusive=TRUE.
     * The unused {@code costOfTermPercent} parameter is ignored (kept for caller signature parity).
     */
    public static FinanceCalculationResult calculate(
            BigDecimal principal,
            BigDecimal profitRate,
            BigDecimal costOfTermPercent,
            int tenureMonths,
            BigDecimal processingFee,
            BigDecimal adminFee
    ) {
        return calculate(principal, profitRate, tenureMonths, processingFee, adminFee,
                DEFAULT_VAT_PERCENTAGE, true);
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

        var availableForInstallment = salary
                .multiply(maxDbrPercent)
                .divide(HUNDRED, SCALE, RM)
                .subtract(existingObligations);

        if (availableForInstallment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        var rateFactor = BigDecimal.ONE.add(
                profitRate.multiply(BigDecimal.valueOf(tenureMonths)).divide(TWELVE, SCALE, RM)
        );

        return availableForInstallment
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(rateFactor, MONEY_SCALE, RM);
    }

    /**
     * Reverse-calculate max loan amount from a given max affordable installment.
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
