package com.ksa.financing.domain.sharia;

import com.ksa.financing.domain.valueobject.ProfitRate;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.Tenure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stateless utility for generating amortization schedules for Islamic financing products.
 * <p>
 * <b>Sharia Compliance:</b> Islamic financing uses flat profit distribution (not compound interest).
 * The total profit is predetermined and known at contract inception, then distributed across
 * installments. This differs from conventional interest-based loans where interest compounds.
 * </p>
 * <p>
 * <b>Key Principles:</b>
 * <ul>
 *   <li>Total Profit = Principal × Profit Rate (calculated once at start)</li>
 *   <li>Total Amount = Principal + Total Profit</li>
 *   <li>Each installment contains both principal and profit components</li>
 *   <li>Profit is distributed evenly (flat) or proportionally across tenure</li>
 *   <li>No compounding - profit does not accrue on unpaid profit</li>
 *   <li>Schedule must be disclosed to customer at contract signing (AAOIFI FAS 28)</li>
 * </ul>
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public final class AmortizationScheduleGenerator {

    // Private constructor to prevent instantiation
    private AmortizationScheduleGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generate a flat amortization schedule for Murabaha/Tawarruq financing.
     * <p>
     * In flat rate calculation:
     * <ul>
     *   <li>Total profit is calculated upfront: Principal × Rate</li>
     *   <li>Total amount (principal + profit) is divided equally across installments</li>
     *   <li>Each installment reduces principal; profit component is proportional</li>
     * </ul>
     * </p>
     *
     * @param principal  The principal amount (cost price)
     * @param profitRate The annual profit rate
     * @param tenure     The financing tenure
     * @param startDate  The contract start date (first installment due one month later)
     * @return List of installment lines
     */
    public static List<InstallmentLine> generateFlatSchedule(
            SarMoney principal,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");

        if (principal.isZero() || principal.isNegative()) {
            throw new IllegalArgumentException("Principal must be positive");
        }

        // Calculate total profit (flat)
        SarMoney totalProfit = profitRate.multiply(principal);
        SarMoney totalAmount = principal.add(totalProfit).add(feeAmount);

        // Calculate fixed monthly installment
        SarMoney monthlyInstallment = totalAmount.divide(tenure.months());

        // Calculate principal per installment (distributed evenly)
        SarMoney principalPerInstallment = principal.divide(tenure.months());

        // Calculate fee per installment (distributed evenly)
        SarMoney feePerInstallment = feeAmount.divide(tenure.months());

        // Derive profit per installment from total minus (principal + fee) to avoid rounding mismatch
        SarMoney profitPerInstallment = monthlyInstallment.subtract(principalPerInstallment).subtract(feePerInstallment);

        List<InstallmentLine> schedule = new ArrayList<>();
        SarMoney remainingPrincipal = principal;
        SarMoney cumulativePrincipal = SarMoney.zero();
        SarMoney cumulativeProfit = SarMoney.zero();

        for (int i = 1; i <= tenure.months(); i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            SarMoney principalComponent;
            SarMoney profitComponent;
            SarMoney feeComponent;
            SarMoney installmentAmount;

            // For the last installment, adjust for any rounding differences
            if (i == tenure.months()) {
                principalComponent = remainingPrincipal;
                feeComponent = feeAmount.subtract(feePerInstallment.multiply(i - 1));
                profitComponent = totalProfit.subtract(cumulativeProfit);
                installmentAmount = principalComponent.add(profitComponent).add(feeComponent);
            } else {
                principalComponent = principalPerInstallment;
                feeComponent = feePerInstallment;
                profitComponent = profitPerInstallment;
                installmentAmount = monthlyInstallment;
            }

            SarMoney openingPrincipal = remainingPrincipal;
            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            cumulativePrincipal = cumulativePrincipal.add(principalComponent);
            cumulativeProfit = cumulativeProfit.add(profitComponent);

            InstallmentLine line = new InstallmentLine(
                    i,
                    dueDate,
                    openingPrincipal,
                    principalComponent,
                    profitComponent,
                    feeComponent,
                    installmentAmount,
                    remainingPrincipal,
                    cumulativePrincipal,
                    cumulativeProfit
            );

            schedule.add(line);
        }

        return List.copyOf(schedule);
    }

    /**
     * Generate a declining balance schedule (for reference - less common in Islamic finance).
     * <p>
     * This applies profit to the remaining principal balance each period, similar to
     * conventional reducing balance but without compounding (profit on profit).
     * </p>
     *
     * @param principal  The principal amount
     * @param profitRate The annual profit rate
     * @param tenure     The financing tenure
     * @param startDate  The contract start date
     * @return List of installment lines
     */
    public static List<InstallmentLine> generateDecliningBalanceSchedule(
            SarMoney principal,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");

        if (principal.isZero() || principal.isNegative()) {
            throw new IllegalArgumentException("Principal must be positive");
        }

        // Monthly profit rate
        BigDecimal monthlyRate = profitRate.asDecimal()
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        // Calculate fixed monthly payment using annuity formula
        // PMT = P × [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(tenure.months());
        BigDecimal numerator = monthlyRate.multiply(onePlusRatePowerN);
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);

        SarMoney monthlyPayment = principal.multiply(numerator.divide(denominator, 2, RoundingMode.HALF_UP));
        
        // Add monthly fee component to the payment
        SarMoney monthlyFee = feeAmount.divide(tenure.months());
        SarMoney totalMonthlyPayment = monthlyPayment.add(monthlyFee);

        List<InstallmentLine> schedule = new ArrayList<>();
        SarMoney remainingPrincipal = principal;
        SarMoney cumulativePrincipal = SarMoney.zero();
        SarMoney cumulativeProfit = SarMoney.zero();

        for (int i = 1; i <= tenure.months(); i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            // Profit on remaining principal
            SarMoney profitComponent = remainingPrincipal.multiply(monthlyRate);

            // Principal component
            SarMoney principalComponent = monthlyPayment.subtract(profitComponent);

            // For last installment, adjust for rounding
            if (i == tenure.months()) {
                principalComponent = remainingPrincipal;
                profitComponent = monthlyPayment.subtract(principalComponent);
                // Last installment gets the remainder of the fee
                monthlyFee = feeAmount.subtract(monthlyFee.multiply(tenure.months() - 1));
                totalMonthlyPayment = principalComponent.add(profitComponent).add(monthlyFee);
            }

            SarMoney openingPrincipal = remainingPrincipal;
            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            cumulativePrincipal = cumulativePrincipal.add(principalComponent);
            cumulativeProfit = cumulativeProfit.add(profitComponent);

            // Ensure non-negative
            if (remainingPrincipal.isNegative()) {
                remainingPrincipal = SarMoney.zero();
            }

            InstallmentLine line = new InstallmentLine(
                    i,
                    dueDate,
                    openingPrincipal,
                    principalComponent,
                    profitComponent,
                    monthlyFee,
                    totalMonthlyPayment,
                    remainingPrincipal,
                    cumulativePrincipal,
                    cumulativeProfit
            );

            schedule.add(line);
        }

        return List.copyOf(schedule);
    }

    /**
     * Generate a custom schedule with specific profit distribution.
     * <p>
     * Allows custom profit amounts per installment (must sum to total profit).
     * Useful for special arrangements or promotional periods.
     * </p>
     *
     * @param principal       The principal amount
     * @param totalProfit     The total profit amount
     * @param profitSchedule  Array of profit amounts per installment (must sum to totalProfit)
     * @param startDate       The contract start date
     * @return List of installment lines
     */
    public static List<InstallmentLine> generateCustomSchedule(
            SarMoney principal,
            SarMoney totalProfit,
            SarMoney[] profitSchedule,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(totalProfit, "Total profit cannot be null");
        Objects.requireNonNull(profitSchedule, "Profit schedule cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null");

        if (profitSchedule.length < 1) {
            throw new IllegalArgumentException("Profit schedule must have at least one installment");
        }

        // Verify profit schedule sums to total profit
        SarMoney profitSum = SarMoney.zero();
        for (SarMoney profit : profitSchedule) {
            profitSum = profitSum.add(profit);
        }
        if (!profitSum.equals(totalProfit)) {
            throw new IllegalArgumentException(
                    String.format("Profit schedule must sum to total profit. Expected: %s, Got: %s",
                            totalProfit, profitSum)
            );
        }

        int tenure = profitSchedule.length;
        SarMoney principalPerInstallment = principal.divide(tenure);
        SarMoney feePerInstallment = feeAmount.divide(tenure);

        List<InstallmentLine> schedule = new ArrayList<>();
        SarMoney remainingPrincipal = principal;
        SarMoney cumulativePrincipal = SarMoney.zero();
        SarMoney cumulativeProfit = SarMoney.zero();

        for (int i = 1; i <= tenure; i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            SarMoney profitComponent = profitSchedule[i - 1];
            SarMoney principalComponent;
            SarMoney monthlyFee = feePerInstallment;

            // For last installment, use remaining principal
            if (i == tenure) {
                principalComponent = remainingPrincipal;
                // Last installment gets the remainder of the fee
                monthlyFee = feeAmount.subtract(feePerInstallment.multiply(tenure - 1));
            } else {
                principalComponent = principalPerInstallment;
            }

            SarMoney installmentAmount = principalComponent.add(profitComponent).add(monthlyFee);

            SarMoney openingPrincipal = remainingPrincipal;
            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            cumulativePrincipal = cumulativePrincipal.add(principalComponent);
            cumulativeProfit = cumulativeProfit.add(profitComponent);

            InstallmentLine line = new InstallmentLine(
                    i,
                    dueDate,
                    openingPrincipal,
                    principalComponent,
                    profitComponent,
                    monthlyFee,
                    installmentAmount,
                    remainingPrincipal,
                    cumulativePrincipal,
                    cumulativeProfit
            );

            schedule.add(line);
        }

        return List.copyOf(schedule);
    }

    /**
     * Calculate total profit for given principal, rate, and tenure.
     *
     * @param principal  The principal amount
     * @param profitRate The annual profit rate
     * @return The total profit amount
     */
    public static SarMoney calculateTotalProfit(SarMoney principal, ProfitRate profitRate) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        return profitRate.multiply(principal);
    }

    /**
     * Calculate monthly installment for flat rate schedule.
     *
     * @param principal  The principal amount
     * @param profitRate The annual profit rate
     * @param tenure     The financing tenure
     * @return The monthly installment amount
     */
    public static SarMoney calculateMonthlyInstallment(
            SarMoney principal,
            ProfitRate profitRate,
            Tenure tenure,
            SarMoney feeAmount
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null");

        SarMoney totalProfit = calculateTotalProfit(principal, profitRate);
        SarMoney totalAmount = principal.add(totalProfit).add(feeAmount);
        return totalAmount.divide(tenure.months());
    }
}
