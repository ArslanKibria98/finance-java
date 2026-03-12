package com.ksa.islamic.orchestration.activity.product;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

/**
 * Step 2: Validates Sharia compliance of product configuration.
 * Checks: valid Sharia structure, profit rate within bounds, commodity config, etc.
 * Runs inside product-service.
 */
@ActivityInterface
public interface ShariaComplianceActivity {

    @ActivityMethod
    ShariaCheckResult checkCompliance(ShariaCheckInput input);

    record ShariaCheckInput(
            String tenantId,
            String productId,
            String productCode,
            String shariaStructure,
            String productType,
            BigDecimal baseProfitRate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int minTenureMonths,
            int maxTenureMonths,
            String currency
    ) {}

    record ShariaCheckResult(
            boolean compliant,
            String complianceLevel,
            String rejectionReason,
            String shariaApprovalReference
    ) {}
}
