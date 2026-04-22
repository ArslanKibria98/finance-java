package com.ksa.financing.lending.adapter.rest.response;

import com.ksa.financing.lending.domain.port.in.SuggestProductsUseCase.EligibleProduct;
import com.ksa.financing.lending.domain.port.in.SuggestProductsUseCase.SuggestProductsResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Product suggestions with installment preview per eligible product")
public record SuggestProductsResponse(
        BigDecimal dbrBefore,
        BigDecimal totalExpenses,
        int productsEvaluated,
        int productsEligible,
        List<EligibleProductResponse> eligibleProducts
) {
    public static SuggestProductsResponse from(SuggestProductsResult result) {
        var mapped = result.eligibleProducts().stream()
                .map(EligibleProductResponse::from)
                .toList();
        return new SuggestProductsResponse(
                result.dbrBefore(),
                result.totalExpenses(),
                result.productsEvaluated(),
                mapped.size(),
                mapped
        );
    }

    public record EligibleProductResponse(
            UUID productId,
            String productCode,
            String productName,
            String productType,
            String shariaStructure,
            BigDecimal profitRate,
            BigDecimal apr,
            BigDecimal amount,
            int tenureMonths,
            int numInstallments,
            BigDecimal monthlyInstallment,
            BigDecimal totalPayable,
            BigDecimal costOfTerm,
            BigDecimal dbrAfter,
            BigDecimal disposableIncome,
            LocalDate firstInstallmentDueDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int minTenureMonths,
            int maxTenureMonths
    ) {
        public static EligibleProductResponse from(EligibleProduct p) {
            return new EligibleProductResponse(
                    p.productId(),
                    p.productCode(),
                    p.productName(),
                    p.productType(),
                    p.shariaStructure(),
                    p.profitRate(),
                    p.apr(),
                    p.amount(),
                    p.tenureMonths(),
                    p.numInstallments(),
                    p.monthlyInstallment(),
                    p.totalPayable(),
                    p.costOfTerm(),
                    p.dbrAfter(),
                    p.disposableIncome(),
                    p.firstInstallmentDueDate(),
                    p.minAmount(),
                    p.maxAmount(),
                    p.minTenureMonths(),
                    p.maxTenureMonths()
            );
        }
    }
}
