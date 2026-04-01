package com.ksa.financing.lending.adapter.rest.response;

import com.ksa.financing.lending.domain.service.FinanceCalculationService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Pre-qualification / finance calculation data")
public record PreQualificationData(
        BigDecimal requestedAmount,
        int tenureMonths,
        BigDecimal monthlyInstallment,
        BigDecimal totalPayable,
        BigDecimal totalProfit,
        BigDecimal profitRate,
        BigDecimal apr,
        int numInstallments,
        String firstInstallmentDueDate,
        BigDecimal processingFee,
        BigDecimal adminFee
) {

    /**
     * Calculate pre-qualification data from amount, tenure, and profit rate.
     * Returns null only if amount or tenure is invalid.
     */
    public static PreQualificationData calculate(BigDecimal amount, int tenureMonths, BigDecimal profitRate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return null;
        }
        if (profitRate == null) profitRate = new BigDecimal("0.025");
        else if (profitRate.compareTo(BigDecimal.ONE) > 0) profitRate = profitRate.movePointLeft(2);

        try {
            var calc = FinanceCalculationService.calculate(
                    amount, profitRate, null, tenureMonths, null, null);
            if (calc.hasErrors()) {
                return new PreQualificationData(amount, tenureMonths, null, amount,
                        BigDecimal.ZERO, profitRate, null, tenureMonths, null,
                        BigDecimal.ZERO, BigDecimal.ZERO);
            }
            return new PreQualificationData(
                    amount, tenureMonths,
                    calc.monthlyInstallment(),
                    calc.totalPayable(),
                    calc.totalCostOfFinancing(),
                    calc.profitRate(),
                    calc.apr(),
                    calc.numInstallments(),
                    calc.firstInstallmentDueDate() != null ? calc.firstInstallmentDueDate().toString() : null,
                    calc.processingFee(),
                    calc.adminFee()
            );
        } catch (Exception e) {
            return new PreQualificationData(amount, tenureMonths, null, amount,
                    BigDecimal.ZERO, profitRate, null, tenureMonths, null,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
