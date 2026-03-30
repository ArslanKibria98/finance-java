package com.ksa.financing.lending.adapter.rest.response;

import com.ksa.financing.lending.domain.service.FinanceCalculationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "BRD UC#01 — Finance Calculator result")
public record FinanceCalculatorResponse(

        @Schema(description = "Whether the request is valid for this product")
        boolean eligible,

        @Schema(description = "Monthly installment in SAR")
        BigDecimal monthlyInstallment,

        @Schema(description = "Finance tenure in months")
        int tenure,

        @Schema(description = "Number of installments")
        int numInstallments,

        @Schema(description = "Cost of term in SAR (profit amount)")
        BigDecimal costOfTerm,

        @Schema(description = "Total cost of financing in SAR")
        BigDecimal totalCostOfFinancing,

        @Schema(description = "First installment due date (30 days from today)")
        LocalDate firstInstallmentDueDate,

        @Schema(description = "Total payable amount in SAR")
        BigDecimal totalPayable,

        @Schema(description = "Processing fee in SAR")
        BigDecimal processingFee,

        @Schema(description = "Administrative fee in SAR")
        BigDecimal adminFee,

        @Schema(description = "Annual profit rate (decimal)")
        BigDecimal profitRate,

        @Schema(description = "Annual Percentage Rate (%)")
        BigDecimal apr,

        @Schema(description = "Validation errors (empty if eligible)")
        List<String> errors
) {
    public static FinanceCalculatorResponse from(FinanceCalculationResult result) {
        return new FinanceCalculatorResponse(
                true,
                result.monthlyInstallment(),
                result.tenureMonths(),
                result.numInstallments(),
                result.costOfTerm(),
                result.totalCostOfFinancing(),
                result.firstInstallmentDueDate(),
                result.totalPayable(),
                result.processingFee(),
                result.adminFee(),
                result.profitRate(),
                result.apr(),
                List.of()
        );
    }

    public static FinanceCalculatorResponse rejected(List<String> errors) {
        return new FinanceCalculatorResponse(
                false, null, 0, 0, null, null, null, null, null, null, null, null, errors
        );
    }
}
