package com.ksa.financing.lending.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Input port for financial-first product suggestion.
 * Takes customer financial info (no productId) and returns all ACTIVE products
 * for which the customer is eligible with per-product installment preview.
 *
 * <p>Stateless: no DB writes, no workflow. Runs the same affordability model
 * as {@link CheckEligibilityUseCase} across every active product in the tenant.
 */
public interface SuggestProductsUseCase {

    SuggestProductsResult suggestProducts(SuggestProductsCommand command);

    record SuggestProductsCommand(
            UUID tenantId,
            String authToken,
            BigDecimal salary,
            BigDecimal liabilities,
            int adultDependents,
            int childDependents,
            BigDecimal foodGroceries,
            BigDecimal utilities,
            BigDecimal healthcare,
            BigDecimal communication,
            BigDecimal housingRent,
            BigDecimal clothingEssentials,
            BigDecimal education,
            BigDecimal transportation
    ) {}

    record SuggestProductsResult(
            BigDecimal dbrBefore,
            BigDecimal totalExpenses,
            int productsEvaluated,
            List<EligibleProduct> eligibleProducts
    ) {}

    record EligibleProduct(
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
    ) {}
}
