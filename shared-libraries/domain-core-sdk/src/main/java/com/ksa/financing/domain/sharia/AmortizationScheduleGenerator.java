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
 * Stateless utility for generating amortization schedules using the reducing-balance
 * methodology defined in {@code docs/reducing-balance-load-documentation.md}.
 *
 * <p>Formula:
 * <pre>
 *   r            = annualRate / 12
 *   EMI          = P × [ r(1+r)^n ] / [ (1+r)^n − 1 ]
 *   profit[i]    = openingBalance[i] × r
 *   principal[i] = EMI − profit[i]
 *   closing[i]   = opening[i] − principal[i]
 * </pre>
 *
 * <p>Sharia note: declining-balance EMI is permissible when the EMI is fixed and the
 * total profit is disclosed at contract inception. Profit does NOT compound onto
 * unpaid profit; it is calculated only on outstanding principal. Late payments do not
 * increase the total profit (penalty handled separately via {@link CharityPenaltyCalculator}).
 *
 * <p>Fee handling:
 * <ul>
 *   <li>Inclusive disbursement: caller deducts fees at disbursement; pass {@code SarMoney.zero()}
 *       to the schedule so fee column is zero per installment.</li>
 *   <li>Non-inclusive: fees are distributed evenly across installments as a separate fee
 *       component on top of principal+profit EMI.</li>
 * </ul>
 */
public final class AmortizationScheduleGenerator {

    private static final int RATE_SCALE = 10;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private AmortizationScheduleGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Canonical reducing-balance schedule generator (matches doc §3–§7).
     *
     * @param principal  Principal amount (cost price)
     * @param profitRate Annual profit rate (decimal, e.g. 0.05 for 5%)
     * @param tenure     Financing tenure in months
     * @param startDate  Contract start date (first installment due one month later)
     * @param feeAmount  Total fees to distribute across installments (use zero for inclusive disbursement)
     */
    public static List<InstallmentLine> generateReducingBalanceSchedule(
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
        Objects.requireNonNull(feeAmount, "Fee amount cannot be null");

        if (principal.isZero() || principal.isNegative()) {
            throw new IllegalArgumentException("Principal must be positive");
        }

        int n = tenure.months();
        BigDecimal monthlyRate = profitRate.asDecimal().divide(TWELVE, RATE_SCALE, RM);

        // EMI on principal+profit only (fee added as separate component)
        BigDecimal emiPrincipalProfit = annuityEmi(principal.getValue(), monthlyRate, n);

        SarMoney feePerInstallment = feeAmount.isZero()
                ? SarMoney.zero()
                : feeAmount.divide(n);

        List<InstallmentLine> schedule = new ArrayList<>();
        SarMoney remainingPrincipal = principal;
        SarMoney cumulativePrincipal = SarMoney.zero();
        SarMoney cumulativeProfit = SarMoney.zero();
        SarMoney cumulativeFee = SarMoney.zero();

        for (int i = 1; i <= n; i++) {
            LocalDate dueDate = startDate.plusMonths(i);

            // Monthly profit on remaining principal
            SarMoney profitComponent = remainingPrincipal.multiply(monthlyRate);

            SarMoney principalComponent;
            SarMoney feeComponent;

            if (i == n) {
                // Last installment absorbs rounding: clear remaining principal and fee remainder
                principalComponent = remainingPrincipal;
                feeComponent = feeAmount.isZero()
                        ? SarMoney.zero()
                        : feeAmount.subtract(cumulativeFee);
            } else {
                principalComponent = SarMoney.of(emiPrincipalProfit).subtract(profitComponent);
                feeComponent = feePerInstallment;
            }

            SarMoney installmentAmount = principalComponent.add(profitComponent).add(feeComponent);

            SarMoney openingPrincipal = remainingPrincipal;
            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            if (remainingPrincipal.isNegative()) {
                remainingPrincipal = SarMoney.zero();
            }
            cumulativePrincipal = cumulativePrincipal.add(principalComponent);
            cumulativeProfit = cumulativeProfit.add(profitComponent);
            cumulativeFee = cumulativeFee.add(feeComponent);

            schedule.add(new InstallmentLine(
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
            ));
        }

        return List.copyOf(schedule);
    }

    /**
     * @deprecated The platform now uses reducing-balance for all products. This method
     * delegates to {@link #generateReducingBalanceSchedule(SarMoney, ProfitRate, Tenure, LocalDate, SarMoney)}.
     * Kept for backward compatibility with existing callers (Murabaha/Tawarruq calculators, Kafka listeners).
     */
    @Deprecated
    public static List<InstallmentLine> generateFlatSchedule(
            SarMoney principal,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        return generateReducingBalanceSchedule(principal, profitRate, tenure, startDate, feeAmount);
    }

    /**
     * @deprecated Alias for {@link #generateReducingBalanceSchedule(SarMoney, ProfitRate, Tenure, LocalDate, SarMoney)}.
     */
    @Deprecated
    public static List<InstallmentLine> generateDecliningBalanceSchedule(
            SarMoney principal,
            ProfitRate profitRate,
            Tenure tenure,
            LocalDate startDate,
            SarMoney feeAmount
    ) {
        return generateReducingBalanceSchedule(principal, profitRate, tenure, startDate, feeAmount);
    }

    /**
     * Custom-distribution schedule kept for special arrangements (promo periods, step-up rates).
     * Profit distribution is supplied by the caller; principal is split evenly.
     * Not a reducing-balance schedule.
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

            if (i == tenure) {
                principalComponent = remainingPrincipal;
                monthlyFee = feeAmount.subtract(feePerInstallment.multiply(tenure - 1));
            } else {
                principalComponent = principalPerInstallment;
            }

            SarMoney installmentAmount = principalComponent.add(profitComponent).add(monthlyFee);

            SarMoney openingPrincipal = remainingPrincipal;
            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            cumulativePrincipal = cumulativePrincipal.add(principalComponent);
            cumulativeProfit = cumulativeProfit.add(profitComponent);

            schedule.add(new InstallmentLine(
                    i, dueDate, openingPrincipal, principalComponent, profitComponent,
                    monthlyFee, installmentAmount, remainingPrincipal,
                    cumulativePrincipal, cumulativeProfit
            ));
        }

        return List.copyOf(schedule);
    }

    /**
     * Total profit under reducing balance = sum of monthly profits on declining outstanding.
     * Walks the schedule logic to derive the figure without allocating installment objects.
     */
    public static SarMoney calculateTotalProfit(
            SarMoney principal,
            ProfitRate profitRate,
            Tenure tenure
    ) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        Objects.requireNonNull(tenure, "Tenure cannot be null");

        int n = tenure.months();
        BigDecimal monthlyRate = profitRate.asDecimal().divide(TWELVE, RATE_SCALE, RM);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return SarMoney.zero();
        }

        BigDecimal emi = annuityEmi(principal.getValue(), monthlyRate, n);
        SarMoney outstanding = principal;
        SarMoney totalProfit = SarMoney.zero();

        for (int i = 1; i <= n; i++) {
            SarMoney profit = outstanding.multiply(monthlyRate);
            SarMoney principalPortion;
            if (i == n) {
                principalPortion = outstanding;
            } else {
                principalPortion = SarMoney.of(emi).subtract(profit);
            }
            outstanding = outstanding.subtract(principalPortion);
            if (outstanding.isNegative()) {
                outstanding = SarMoney.zero();
            }
            totalProfit = totalProfit.add(profit);
        }
        return totalProfit;
    }

    /**
     * Two-arg overload kept for callers that lack a {@link Tenure}. Returns SarMoney.zero()
     * since reducing-balance profit cannot be computed without tenure.
     *
     * @deprecated Pass {@link Tenure} to get a meaningful total profit.
     */
    @Deprecated
    public static SarMoney calculateTotalProfit(SarMoney principal, ProfitRate profitRate) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        Objects.requireNonNull(profitRate, "Profit rate cannot be null");
        return SarMoney.zero();
    }

    /** Reducing-balance EMI (principal+profit only). */
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

        BigDecimal monthlyRate = profitRate.asDecimal().divide(TWELVE, RATE_SCALE, RM);
        BigDecimal emi = annuityEmi(principal.getValue(), monthlyRate, tenure.months());

        // When fees are non-inclusive (caller chose to add to schedule), distribute evenly.
        SarMoney feePerInstallment = feeAmount.isZero()
                ? SarMoney.zero()
                : feeAmount.divide(tenure.months());

        return SarMoney.of(emi).add(feePerInstallment);
    }

    /**
     * Annuity EMI on principal+profit:
     *   EMI = P × r(1+r)^n / ((1+r)^n − 1)
     * Falls back to P/n when r == 0.
     */
    private static BigDecimal annuityEmi(BigDecimal principal, BigDecimal monthlyRate, int n) {
        if (n <= 0) {
            return BigDecimal.ZERO;
        }
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RM);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(n);
        BigDecimal numerator = monthlyRate.multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        return principal.multiply(numerator).divide(denominator, 2, RM);
    }
}
