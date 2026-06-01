package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reducing-balance amortization schedule builder, doc-compliant
 * (see {@code docs/reducing-balance-load-documentation.md}).
 *
 * <p>Algorithm (per doc §3, §5):
 * <ul>
 *   <li>monthlyRate r = annualRate / 12</li>
 *   <li>EMI = P × r(1+r)^n / ((1+r)^n − 1) — annuity, constant across installments</li>
 *   <li>profit[i] = openingOutstanding[i] × r (declines monthly)</li>
 *   <li>principal[i] = EMI − profit[i] (increases monthly)</li>
 *   <li>last installment: principal = remaining outstanding (absorbs rounding)</li>
 * </ul>
 *
 * <p>Fee distribution: proportional to monthly profit; last installment absorbs fee remainder.
 * <p>payableAmount = principal + profit + fee per row.
 * <p>totalInstallment (EMI) = principal + profit (constant per doc §4).
 */
public final class ReducingBalanceScheduleBuilder {

    private static final int RATE_SCALE = 10;
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private ReducingBalanceScheduleBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }

    public record InstallmentRow(
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal openingPrincipal,
            BigDecimal principalComponent,
            BigDecimal profitComponent,
            BigDecimal feeComponent,
            BigDecimal totalInstallment,   // EMI = principal + profit (constant under reducing balance)
            BigDecimal payableAmount,      // principal + profit + fee
            BigDecimal closingPrincipal,
            BigDecimal cumulativePrincipal,
            BigDecimal cumulativeProfit
    ) {}

    public static List<InstallmentRow> build(
            BigDecimal principal,
            BigDecimal annualRate,
            BigDecimal totalProfit,    // unused (recomputed via walk); kept for backward compat
            BigDecimal totalFee,
            int tenure,
            LocalDate firstDueDate
    ) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (tenure <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }
        if (firstDueDate == null) {
            throw new IllegalArgumentException("First due date cannot be null");
        }

        BigDecimal rate = annualRate != null ? annualRate : BigDecimal.ZERO;
        BigDecimal monthlyRate = rate.divide(TWELVE, RATE_SCALE, RM);
        BigDecimal safeTotalFee = totalFee != null ? totalFee : BigDecimal.ZERO;

        // Annuity EMI per doc §3
        BigDecimal emi = annuityEmi(principal, monthlyRate, tenure);

        // First pass: walk schedule to compute per-month profit and principal
        BigDecimal[] profitPerMonth = new BigDecimal[tenure];
        BigDecimal[] principalPerMonth = new BigDecimal[tenure];
        BigDecimal[] openingPerMonth = new BigDecimal[tenure];
        BigDecimal outstanding = principal;
        BigDecimal walkedProfit = BigDecimal.ZERO;
        for (int i = 0; i < tenure; i++) {
            openingPerMonth[i] = outstanding;
            BigDecimal profit = outstanding.multiply(monthlyRate).setScale(MONEY_SCALE, RM);
            BigDecimal principalPortion;
            if (i == tenure - 1) {
                principalPortion = outstanding;             // last absorbs principal remainder
            } else {
                principalPortion = emi.subtract(profit);
            }
            profitPerMonth[i] = profit;
            principalPerMonth[i] = principalPortion;
            outstanding = outstanding.subtract(principalPortion);
            if (outstanding.signum() < 0) outstanding = BigDecimal.ZERO;
            walkedProfit = walkedProfit.add(profit);
        }

        // Fee distribution proportional to profit (or even split when profit is zero)
        BigDecimal[] feePerMonth = new BigDecimal[tenure];
        BigDecimal cumulativeFee = BigDecimal.ZERO;
        boolean profitIsZero = walkedProfit.compareTo(BigDecimal.ZERO) == 0;
        for (int i = 0; i < tenure; i++) {
            if (i == tenure - 1) {
                feePerMonth[i] = safeTotalFee.subtract(cumulativeFee).setScale(MONEY_SCALE, RM);
            } else if (safeTotalFee.compareTo(BigDecimal.ZERO) == 0) {
                feePerMonth[i] = BigDecimal.ZERO.setScale(MONEY_SCALE);
            } else if (profitIsZero) {
                feePerMonth[i] = safeTotalFee.divide(BigDecimal.valueOf(tenure), MONEY_SCALE, RM);
            } else {
                BigDecimal share = profitPerMonth[i].divide(walkedProfit, RATE_SCALE, RM);
                feePerMonth[i] = safeTotalFee.multiply(share).setScale(MONEY_SCALE, RM);
            }
            cumulativeFee = cumulativeFee.add(feePerMonth[i]);
        }

        // Second pass: assemble rows
        List<InstallmentRow> rows = new ArrayList<>(tenure);
        BigDecimal currentOutstanding = principal;
        BigDecimal cumPrincipal = BigDecimal.ZERO;
        BigDecimal cumProfit = BigDecimal.ZERO;
        for (int i = 0; i < tenure; i++) {
            BigDecimal opening = openingPerMonth[i];
            BigDecimal principalPortion = principalPerMonth[i];
            BigDecimal profit = profitPerMonth[i];
            BigDecimal fee = feePerMonth[i];
            BigDecimal totalInstallment = principalPortion.add(profit);     // EMI (principal + profit)
            BigDecimal payable = totalInstallment.add(fee);                  // including fee
            currentOutstanding = currentOutstanding.subtract(principalPortion);
            if (currentOutstanding.signum() < 0) currentOutstanding = BigDecimal.ZERO;
            cumPrincipal = cumPrincipal.add(principalPortion);
            cumProfit = cumProfit.add(profit);

            rows.add(new InstallmentRow(
                    i + 1,
                    firstDueDate.plusMonths(i),
                    opening,
                    principalPortion,
                    profit,
                    fee,
                    totalInstallment,                // EMI = principal + profit
                    payable,
                    currentOutstanding,
                    cumPrincipal,
                    cumProfit
            ));
        }
        return rows;
    }

    /**
     * Annuity EMI per doc §3:
     *   EMI = P × r(1+r)^n / ((1+r)^n − 1)
     * Falls back to P/n when r == 0.
     */
    private static BigDecimal annuityEmi(BigDecimal principal, BigDecimal monthlyRate, int n) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), MONEY_SCALE, RM);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(n);
        BigDecimal numerator = monthlyRate.multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        return principal.multiply(numerator).divide(denominator, MONEY_SCALE, RM);
    }
}
