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
    public static PreQualificationData calculateFinance(BigDecimal amount, int tenureMonths, BigDecimal profitRate,
                                                 BigDecimal processingFeePercent, BigDecimal processingFeeAmount, BigDecimal adminFeeAmount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return null;
        }
        if (profitRate == null) profitRate = new BigDecimal("0.0385");
        else if (profitRate.compareTo(new BigDecimal("0.5")) > 0) profitRate = profitRate.movePointLeft(2);

        // Calculate processing fee: prioritize fixed amount, then percentage
        BigDecimal safeProcFee = BigDecimal.ZERO;
        if (processingFeeAmount != null && processingFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
            safeProcFee = processingFeeAmount;
        } else if (processingFeePercent != null && processingFeePercent.compareTo(BigDecimal.ZERO) > 0) {
            safeProcFee = amount.multiply(processingFeePercent).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal safeAdminFee = adminFeeAmount != null ? adminFeeAmount : BigDecimal.ZERO;

        try {
            var calc = FinanceCalculationService.calculate(
                    amount, profitRate, null, tenureMonths, safeProcFee, safeAdminFee);
            if (calc.hasErrors()) {
                return new PreQualificationData(amount, tenureMonths, null, amount,
                        BigDecimal.ZERO, profitRate, null, tenureMonths, null,
                        safeProcFee, safeAdminFee);
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
                    safeProcFee, safeAdminFee);
        }
    }

    /**
     * Internal helper to calculate pre-qualification data using absolute fee amounts.
     */
    public static PreQualificationData calculateWithAmounts(BigDecimal amount, int tenureMonths, BigDecimal profitRate,
                                                         BigDecimal processingFeeAmount, BigDecimal adminFeeAmount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return null;
        }
        if (profitRate == null) profitRate = new BigDecimal("0.0385");
        else if (profitRate.compareTo(new BigDecimal("0.5")) > 0) profitRate = profitRate.movePointLeft(2);

        BigDecimal safeProcFee = processingFeeAmount != null ? processingFeeAmount : BigDecimal.ZERO;
        BigDecimal safeAdminFee = adminFeeAmount != null ? adminFeeAmount : BigDecimal.ZERO;

        try {
            var calc = FinanceCalculationService.calculate(
                    amount, profitRate, null, tenureMonths, safeProcFee, safeAdminFee);

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
                    safeProcFee, safeAdminFee);
        }
    }

    /**
     * Legacy overload for backward compatibility.
     */
    public static PreQualificationData calculate(BigDecimal amount, int tenureMonths, BigDecimal profitRate) {
        return calculateFinance(amount, tenureMonths, profitRate, null, null, null);
    }

    /**
     * Build PreQualificationData from an existing offer to ensure consistency with the flow.
     */
    public static PreQualificationData fromOffer(com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow.OfferDetails offer) {
        if (offer == null) return null;
        return new PreQualificationData(
                offer.selectedAmount() != null ? offer.selectedAmount() : offer.maxAmount(),
                offer.tenureMonths(),
                offer.monthlyInstallment(),
                offer.totalPayable(),
                offer.totalProfit(),
                offer.annualProfitRate(),
                offer.apr(),
                offer.tenureMonths(),
                offer.firstInstallmentDate(),
                offer.processingFee(),
                offer.adminFee()
        );
    }

    /**
     * Build PreQualificationData from a domain aggregate to ensure consistency for completed/historical applications.
     */
    public static PreQualificationData fromAggregate(com.ksa.financing.lending.domain.model.LoanApplicationAggregate agg) {
        if (agg == null) return null;

        // Use offered/accepted values if they exist, otherwise fallback to requested
        BigDecimal amount = agg.getAcceptedAmount() != null ? agg.getAcceptedAmount() :
                           (agg.getOfferedAmount() != null ? agg.getOfferedAmount() : agg.getRequestedAmount());

        BigDecimal totalPayable = agg.getOfferedTotalPayable();
        BigDecimal installment = agg.getOfferedMonthlyInstallment();
        BigDecimal totalProfit = agg.getOfferedTotalProfit();

        // Resolve fees: use Step 1 persisted config fees (processingFeePercent/processingFeeAmount/adminFeeAmount)
        // which are populated from product validation. Fall back to offer-level fees if available.
        BigDecimal procFee = agg.getProcessingFeeAmount();
        if ((procFee == null || procFee.compareTo(BigDecimal.ZERO) == 0) && agg.getProcessingFeePercent() != null && agg.getProcessingFeePercent().compareTo(BigDecimal.ZERO) > 0) {
            procFee = agg.getRequestedAmount().multiply(agg.getProcessingFeePercent()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }
        if ((procFee == null || procFee.compareTo(BigDecimal.ZERO) == 0) && agg.getProcessingFee() != null) {
            procFee = agg.getProcessingFee();
        }
        BigDecimal adminFee = agg.getAdminFeeAmount();
        if ((adminFee == null || adminFee.compareTo(BigDecimal.ZERO) == 0) && agg.getAdminFee() != null) {
            adminFee = agg.getAdminFee();
        }

        // If we don't have offered values yet, calculate from persisted config fees
        if (totalPayable == null) {
            return calculateFinance(agg.getRequestedAmount(), agg.getRequestedTenureMonths(), agg.getProfitRate(),
                                   agg.getProcessingFeePercent(), agg.getProcessingFeeAmount(), agg.getAdminFeeAmount());
        }

        return new PreQualificationData(
                amount,
                agg.getRequestedTenureMonths(),
                installment,
                totalPayable,
                totalProfit,
                agg.getProfitRate(),
                agg.getApr(),
                agg.getRequestedTenureMonths(),
                null,
                procFee != null ? procFee : BigDecimal.ZERO,
                adminFee != null ? adminFee : BigDecimal.ZERO
        );
    }
}
