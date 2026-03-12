package com.ksa.islamic.orchestration.activity.product;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Temporal workflow for product activation lifecycle:
 *
 * <pre>
 * Step 1: Validate Product Configuration
 * Step 2: Sharia Compliance Check
 * Step 3: Create Loan Product in Fineract (CBS)
 * Step 4: Activate Product & Publish Events
 * </pre>
 *
 * <p>SAGA compensation: On failure at any step, previous steps are compensated
 * (Fineract product deactivated, product status reverted to DRAFT).</p>
 */
@WorkflowInterface
public interface ProductActivationWorkflow {

    @WorkflowMethod
    ActivationResult execute(ActivationRequest request);

    @QueryMethod
    ActivationStatus getStatus();

    // ══════════════════════════════════════════════════════════════
    // REQUEST
    // ══════════════════════════════════════════════════════════════

    record ActivationRequest(
            String tenantId,
            String productId,
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
            List<Integer> allowedTenures,
            String activatedBy
    ) {}

    // ══════════════════════════════════════════════════════════════
    // RESULT
    // ══════════════════════════════════════════════════════════════

    record ActivationResult(
            String productId,
            String fineractProductId,
            String status,
            String failureReason,
            boolean success
    ) {}

    // ══════════════════════════════════════════════════════════════
    // QUERY RESPONSE
    // ══════════════════════════════════════════════════════════════

    record ActivationStatus(
            String productId,
            String currentStep,
            String status,
            String errorMessage,
            String fineractProductId
    ) {}
}
