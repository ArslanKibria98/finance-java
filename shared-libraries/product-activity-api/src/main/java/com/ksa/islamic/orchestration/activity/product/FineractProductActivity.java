package com.ksa.islamic.orchestration.activity.product;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Step 3: Creates/syncs the loan product in Apache Fineract (CBS).
 * Uses lms-adapter-sdk to communicate with Fineract.
 * Runs inside product-service.
 */
@ActivityInterface
public interface FineractProductActivity {

    @ActivityMethod
    CreateFineractProductResult createLoanProduct(CreateFineractProductInput input);

    @ActivityMethod
    void deactivateLoanProduct(DeactivateFineractProductInput input);

    record CreateFineractProductInput(
            String tenantId,
            String productId,
            String productCode,
            String nameEn,
            String shariaStructure,
            String currency,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int minTenureMonths,
            int maxTenureMonths,
            BigDecimal baseProfitRate,
            String rateType,
            String repaymentFrequency,
            int gracePeriodDays,
            boolean earlySettlementAllowed,
            List<Integer> allowedTenures,
            String idempotencyKey
    ) {}

    record CreateFineractProductResult(
            String fineractProductId,
            String fineractShortName,
            boolean success,
            String errorMessage
    ) {}

    record DeactivateFineractProductInput(
            String tenantId,
            String fineractProductId
    ) {}
}
