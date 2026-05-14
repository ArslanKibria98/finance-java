package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stateless calculator for Murabaha (cost-plus-profit) financing.
 * <p>
 * <b>Murabaha Definition:</b> A sale contract where the seller (financier) purchases goods
 * and sells them to the buyer (customer) at cost plus an agreed markup. The cost and profit
 * must be disclosed transparently.
 * </p>
 * <p>
 * <b>Sharia Compliance Requirements (AAOIFI FAS 2):</b>
 * <ul>
 *   <li>Financier must purchase and own the asset before selling to customer</li>
 *   <li>Cost price must be disclosed to customer</li>
 *   <li>Profit markup must be known and agreed upon</li>
 *   <li>Asset must be lawful (halal), exist, and be deliverable</li>
 *   <li>Sale price is fixed and cannot increase due to late payment</li>
 *   <li>Deferred payment (installments) is permissible</li>
 *   <li>Risk of ownership temporarily passes to financier</li>
 * </ul>
 * </p>
 * <p>
 * <b>Calculation Formula:</b>
 * <pre>
 * Profit Amount = Cost Price × Profit Rate
 * Sale Price = Cost Price + Profit Amount
 * Monthly Installment = Sale Price ÷ Tenure (months)
 * </pre>
 * </p>
 * <p>
 * <b>Example:</b>
 * <pre>
 * Cost Price: SAR 100,000
 * Profit Rate: 5% (0.05)
 * Tenure: 12 months
 *
 * Profit Amount = 100,000 × 0.05 = SAR 5,000
 * Sale Price = 100,000 + 5,000 = SAR 105,000
 * Monthly Installment = 105,000 ÷ 12 = SAR 8,750
 * </pre>
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class MurabahaCalculator {

    // Private constructor to prevent instantiation
    private MurabahaCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculate complete Murabaha financing with flat profit distribution.
     * <p>
     * This is the standard Murabaha calculation where total profit is predetermined
     * and distributed evenly across all installments.
     * </p>
     *
     * @param costPrice  The cost price of the asset (must be disclosed)
     * @param profitRate The annual profit rate (markup percentage)
     * @param tenure     The financing period in months
     * @param startDate  The contract start date
     * @return Complete Murabaha calculation result
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static MurabahaCalculation calculate(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");

        if (costPrice.isZero() || costPrice.isNegative()) {
            throw new IllegalArgumentException("Cost price must be positive");
        }

        // Calculate profit amount
        SarMoney profitAmount = profitRate.multiply(costPrice);

        // Calculate sale price
        SarMoney salePrice = costPrice.add(profitAmount);

        // Calculate monthly installment (Principal + Profit + Fee)
        SarMoney monthlyInstallment = salePrice.add(feeAmount).divide(tenure.months());

        // Generate amortization schedule
        List<InstallmentLine> schedule = AmortizationScheduleGenerator.generateFlatSchedule(
                costPrice,
                profitRate,
                tenure,
                startDate,
                feeAmount
        );

        return new MurabahaCalculation(
                costPrice,
                profitAmount,
                salePrice,
                monthlyInstallment,
                schedule
        );
    }

    /**
     * Calculate Murabaha financing with declining balance profit distribution.
     * <p>
     * Less common in Islamic finance, but used in some jurisdictions.
     * Profit is calculated on the declining principal balance each period.
     * </p>
     *
     * @param costPrice  The cost price of the asset
     * @param profitRate The annual profit rate
     * @param tenure     The financing period in months
     * @param startDate  The contract start date
     * @return Complete Murabaha calculation result
     */
    public static MurabahaCalculation calculateDecliningBalance(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null");

        if (costPrice.isZero() || costPrice.isNegative()) {
            throw new IllegalArgumentException("Cost price must be positive");
        }

        // Generate declining balance schedule
        List<InstallmentLine> schedule = AmortizationScheduleGenerator.generateDecliningBalanceSchedule(
                costPrice,
                profitRate,
                tenure,
                startDate,
                feeAmount
        );

        // Calculate totals from schedule
        SarMoney totalProfit = schedule.stream()
                .map(InstallmentLine::profitComponent)
                .reduce(SarMoney.zero(), SarMoney::add);

        SarMoney salePrice = costPrice.add(totalProfit);

        // Monthly installment is from schedule (first installment, as they're equal in declining balance)
        SarMoney monthlyInstallment = schedule.get(0).totalInstallment();

        return new MurabahaCalculation(
                costPrice,
                totalProfit,
                salePrice,
                monthlyInstallment,
                schedule
        );
    }

    /**
     * Calculate only the profit amount (without full calculation).
     * <p>
     * Useful for quick profit estimates or validation.
     * </p>
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @return The profit amount
     */
    public static SarMoney calculateProfitAmount(SarMoney costPrice, ProfitRate profitRate) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        return profitRate.multiply(costPrice);
    }

    /**
     * Calculate only the sale price (without full calculation).
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @return The sale price (cost + profit)
     */
    public static SarMoney calculateSalePrice(SarMoney costPrice, ProfitRate profitRate) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        SarMoney profit = calculateProfitAmount(costPrice, profitRate);
        return costPrice.add(profit);
    }

    /**
     * Calculate only the monthly installment (without full calculation).
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @param tenure     The tenure
     * @return The monthly installment amount
     */
    public static SarMoney calculateMonthlyInstallment(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(costPrice, "Cost price cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");

        SarMoney salePrice = calculateSalePrice(costPrice, profitRate);
        return salePrice.add(feeAmount).divide(tenure.months());
    }

    /**
     * Validate if given parameters meet Sharia compliance requirements.
     * <p>
     * Checks for basic validation rules. Does not validate asset legitimacy
     * or ownership - those require domain-specific checks.
     * </p>
     *
     * @param costPrice  The cost price
     * @param profitRate The profit rate
     * @param tenure     The tenure
     * @return true if parameters are valid
     */
    public static boolean isValidForMurabaha(
            SarMoney costPrice,
            ProfitRate profitRate,
            Tenure tenure
    ) {
        try {
            Objects.requireNonNull(costPrice, "Cost price cannot be null");
            Objects.requireNonNull(profitRate, "Profit rate cannot be null");
            Objects.requireNonNull(tenure, "Tenure cannot be null");

            return costPrice.isPositive()
                    && profitRate.value().compareTo(java.math.BigDecimal.ZERO) > 0
                    && tenure.months() >= 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate the effective annual rate (EAR) for comparison purposes.
     * <p>
     * Note: This is for comparison only. Islamic financing uses flat rates,
     * not compound interest rates.
     * </p>
     *
     * @param profitRate The profit rate
     * @return The effective annual rate
     */
    public static double calculateEffectiveAnnualRate(ProfitRate profitRate) {
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        return profitRate.asDecimal().doubleValue();
    }

    /**
     * Calculate monthly payment as percentage of cost price.
     * <p>
     * Useful for affordability calculations.
     * </p>
     *
     * @param profitRate The profit rate
     * @param tenure     The tenure
     * @return Monthly payment as decimal (e.g., 0.0875 = 8.75% of cost price)
     */
    public static double calculateMonthlyPaymentRatio(ProfitRate profitRate, Tenure tenure) {
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");

        double totalRate = 1.0 + profitRate.asDecimal().doubleValue();
        return totalRate / tenure.months();
    }
}
