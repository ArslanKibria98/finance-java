package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of flat-rate Murabaha finance calculation with VAT and IsDisbursementInclusive support.
 *
 * <p>Field mapping:
 * <ul>
 *   <li>{@code totalCostOfFinancing} = total profit before VAT (4-case branch result)</li>
 *   <li>{@code costOfTerm}           = profit from percentage alone (principal × rate × tenure/12)</li>
 *   <li>{@code vatAmount}            = vatOnProfit (vat applied to totalProfitBeforeVat)</li>
 *   <li>{@code profitBeforeVat}      = same as totalCostOfFinancing (alias for clarity)</li>
 *   <li>{@code profitAfterVat}       = profitBeforeVat − vatAmount</li>
 *   <li>{@code totalPayable}         = principal + profitAfterVat + vatAmount</li>
 * </ul>
 */
public record FinanceCalculationResult(
        BigDecimal requestedAmount,
        int tenureMonths,
        int numInstallments,
        BigDecimal monthlyInstallment,
        BigDecimal totalCostOfFinancing,
        BigDecimal costOfTerm,
        BigDecimal totalPayable,
        BigDecimal processingFee,
        BigDecimal adminFee,
        BigDecimal profitRate,
        BigDecimal apr,
        LocalDate firstInstallmentDueDate,
        BigDecimal vatAmount,
        BigDecimal profitBeforeVat,
        BigDecimal profitAfterVat,
        boolean isDisbursementInclusive,
        BigDecimal vatPercentage,
        java.util.List<String> errors
) {

    /** Legacy constructor without VAT/inclusive fields — defaults to zero VAT, inclusive=TRUE. */
    public FinanceCalculationResult(
            BigDecimal requestedAmount, int tenureMonths, int numInstallments,
            BigDecimal monthlyInstallment, BigDecimal totalCostOfFinancing, BigDecimal costOfTerm,
            BigDecimal totalPayable, BigDecimal processingFee, BigDecimal adminFee,
            BigDecimal profitRate, BigDecimal apr, LocalDate firstInstallmentDueDate,
            java.util.List<String> errors) {
        this(requestedAmount, tenureMonths, numInstallments, monthlyInstallment,
                totalCostOfFinancing, costOfTerm, totalPayable, processingFee, adminFee,
                profitRate, apr, firstInstallmentDueDate,
                BigDecimal.ZERO, totalCostOfFinancing, totalCostOfFinancing, true, BigDecimal.ZERO,
                errors);
    }

    /** Legacy constructor without errors list. */
    public FinanceCalculationResult(
            BigDecimal requestedAmount, int tenureMonths, int numInstallments,
            BigDecimal monthlyInstallment, BigDecimal totalCostOfFinancing, BigDecimal costOfTerm,
            BigDecimal totalPayable, BigDecimal processingFee, BigDecimal adminFee,
            BigDecimal profitRate, BigDecimal apr, LocalDate firstInstallmentDueDate) {
        this(requestedAmount, tenureMonths, numInstallments, monthlyInstallment,
                totalCostOfFinancing, costOfTerm, totalPayable, processingFee, adminFee,
                profitRate, apr, firstInstallmentDueDate, java.util.List.of());
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public static FinanceCalculationResult rejected(java.util.List<String> errors) {
        return new FinanceCalculationResult(
                null, 0, 0, null, null, null, null, null, null, null, null, null,
                null, null, null, true, null,
                errors);
    }
}
