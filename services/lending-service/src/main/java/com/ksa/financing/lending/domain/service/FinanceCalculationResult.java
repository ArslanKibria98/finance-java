package com.ksa.financing.lending.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of BRD V1.8 finance calculation.
 * <p>
 * Formula (BRD Page 14, Step 34):
 * <pre>
 * totalCostOfFinancing = principal × profitRate × (tenureMonths / 12)
 * costOfTerm           = totalCostOfFinancing (same as profit in flat-rate Murabaha)
 * totalPayable         = principal + totalCostOfFinancing
 * monthlyInstallment   = totalPayable / tenureMonths
 * firstInstallmentDueDate = today + 30 days
 * APR = (totalCostOfFinancing / principal) / (tenureMonths / 12) × 100
 * </pre>
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
        java.util.List<String> errors
) {
    /** Constructor without errors (backward compatible) */
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
                null, 0, 0, null, null, null, null, null, null, null, null, null, errors);
    }
}
