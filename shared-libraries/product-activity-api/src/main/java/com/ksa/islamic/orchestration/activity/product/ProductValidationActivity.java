package com.ksa.islamic.orchestration.activity.product;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Step 1: Validates product configuration completeness before activation.
 * Runs inside product-service.
 */
@ActivityInterface
public interface ProductValidationActivity {

    @ActivityMethod
    ValidationResult validateForActivation(ValidateProductInput input);

    record ValidateProductInput(
            String tenantId,
            String productId
    ) {}

    record ValidationResult(
            boolean valid,
            List<String> errors,
            String productCode,
            String nameEn,
            String nameAr,
            String productType,
            String shariaStructure,
            String targetSegment,
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
            List<Integer> allowedTenures
    ) {}
}
