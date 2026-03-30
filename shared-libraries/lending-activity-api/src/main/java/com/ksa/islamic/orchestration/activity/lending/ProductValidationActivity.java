package com.ksa.islamic.orchestration.activity.lending;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validates product details by calling product-service.
 * Used in Step 1 (Basic Information) of the loan application workflow.
 */
@ActivityInterface
public interface ProductValidationActivity {

    @ActivityMethod
    ProductValidationResult validateProduct(ProductValidationInput input);

    record ProductValidationInput(
            String tenantId,
            String productId,
            BigDecimal requestedAmount,
            int requestedTenureMonths
    ) {}

    record ProductValidationResult(
            boolean valid,
            String productCode,
            String productName,
            String shariaStructure,
            String fineractProductId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int minTenureMonths,
            int maxTenureMonths,
            BigDecimal profitRate,
            BigDecimal processingFeePercent,
            BigDecimal adminFeeAmount,
            int minAge,
            int maxAge,
            BigDecimal minSalary,
            int minEmploymentMonths,
            int minCreditScore,
            BigDecimal maxDbrPercent,
            List<String> requiredDocuments,
            String rejectionReason      // null if valid
    ) {}
}
