package com.ksa.financing.product.adapter.temporal.workflow;

import com.ksa.islamic.orchestration.activity.product.*;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Product Activation Workflow Implementation with SAGA compensation.
 *
 * <pre>
 * Step 1: Update status → PENDING_ACTIVATION
 * Step 2: Validate product configuration
 * Step 3: Check Sharia compliance
 * Step 4: Create loan product in Fineract (CBS)
 * Step 5: Store Fineract mapping + Activate product
 * </pre>
 *
 * Compensation on failure:
 * - Fineract product deactivated (if created)
 * - Product status reverted to DRAFT
 */
@Slf4j
public class ProductActivationWorkflowImpl implements ProductActivationWorkflow {

    private static final RetryOptions RETRY_OPTIONS = RetryOptions.newBuilder()
            .setMaximumAttempts(3)
            .setInitialInterval(Duration.ofSeconds(2))
            .setMaximumInterval(Duration.ofSeconds(30))
            .setBackoffCoefficient(2.0)
            .build();

    private static final ActivityOptions LOCAL_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RETRY_OPTIONS)
            .build();

    private static final ActivityOptions FINERACT_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setMaximumInterval(Duration.ofMinutes(2))
                    .setBackoffCoefficient(2.0)
                    .build())
            .build();

    private final ProductValidationActivity validationActivity =
            Workflow.newActivityStub(ProductValidationActivity.class, LOCAL_OPTIONS);

    private final ShariaComplianceActivity shariaActivity =
            Workflow.newActivityStub(ShariaComplianceActivity.class, LOCAL_OPTIONS);

    private final FineractProductActivity fineractActivity =
            Workflow.newActivityStub(FineractProductActivity.class, FINERACT_OPTIONS);

    private final ProductLifecycleActivity lifecycleActivity =
            Workflow.newActivityStub(ProductLifecycleActivity.class, LOCAL_OPTIONS);

    // Workflow state for query
    private String currentStep = "INITIALIZED";
    private String status = "IN_PROGRESS";
    private String errorMessage = null;
    private String fineractProductId = null;
    private String productId = null;

    @Override
    public ActivationResult execute(ActivationRequest request) {
        productId = request.productId();
        log.info("Starting product activation workflow: productId={} tenant={}", request.productId(), request.tenantId());

        try {
            // ══════════ Step 1: Set status to PENDING_ACTIVATION ══════════
            currentStep = "PENDING_ACTIVATION";
            lifecycleActivity.updateStatusToPendingActivation(
                    new ProductLifecycleActivity.UpdateStatusInput(request.tenantId(), request.productId()));

            // ══════════ Step 2: Validate product configuration ══════════
            currentStep = "VALIDATING";
            var validationResult = validationActivity.validateForActivation(
                    new ProductValidationActivity.ValidateProductInput(request.tenantId(), request.productId()));

            if (!validationResult.valid()) {
                String reason = "Validation failed: " + String.join(", ", validationResult.errors());
                return handleFailure(request.tenantId(), request.productId(), reason, null);
            }

            // ══════════ Step 3: Sharia compliance check ══════════
            currentStep = "SHARIA_CHECK";
            var shariaResult = shariaActivity.checkCompliance(new ShariaComplianceActivity.ShariaCheckInput(
                    request.tenantId(),
                    request.productId(),
                    request.productCode(),
                    request.shariaStructure(),
                    request.productType(),
                    request.baseProfitRate(),
                    request.minAmount(),
                    request.maxAmount(),
                    request.minTenureMonths(),
                    request.maxTenureMonths(),
                    request.currency()
            ));

            if (!shariaResult.compliant()) {
                String reason = "Sharia compliance failed: " + shariaResult.rejectionReason();
                return handleFailure(request.tenantId(), request.productId(), reason, null);
            }

            // ══════════ Step 4: Create loan product in Fineract ══════════
            currentStep = "FINERACT_CREATION";
            String idempotencyKey = "product-activate-" + request.productId();

            var fineractResult = fineractActivity.createLoanProduct(new FineractProductActivity.CreateFineractProductInput(
                    request.tenantId(),
                    request.productId(),
                    request.productCode(),
                    request.nameEn(),
                    request.shariaStructure(),
                    request.currency(),
                    request.minAmount(),
                    request.maxAmount(),
                    request.minTenureMonths(),
                    request.maxTenureMonths(),
                    request.baseProfitRate(),
                    request.rateType(),
                    request.repaymentFrequency(),
                    request.gracePeriodDays(),
                    request.earlySettlementAllowed(),
                    request.allowedTenures(),
                    idempotencyKey
            ));

            if (!fineractResult.success()) {
                String reason = "Fineract product creation failed: " + fineractResult.errorMessage();
                return handleFailure(request.tenantId(), request.productId(), reason, null);
            }

            fineractProductId = fineractResult.fineractProductId();

            // ══════════ Step 5: Store mapping + activate ══════════
            currentStep = "ACTIVATING";

            // Store the Fineract product ID mapping
            lifecycleActivity.storeFineractMapping(new ProductLifecycleActivity.StoreMappingInput(
                    request.tenantId(),
                    request.productId(),
                    request.productCode(),
                    fineractProductId
            ));

            // Activate the product
            lifecycleActivity.activateProduct(new ProductLifecycleActivity.ActivateProductInput(
                    request.tenantId(),
                    request.productId(),
                    fineractProductId
            ));

            currentStep = "COMPLETED";
            status = "COMPLETED";
            log.info("Product activation workflow completed: productId={} fineractProductId={}",
                    request.productId(), fineractProductId);

            return new ActivationResult(
                    request.productId(),
                    fineractProductId,
                    "ACTIVE",
                    null,
                    true
            );

        } catch (Exception e) {
            log.error("Product activation workflow failed: productId={}", request.productId(), e);
            return handleFailure(request.tenantId(), request.productId(), e.getMessage(), fineractProductId);
        }
    }

    private ActivationResult handleFailure(String tenantId, String productId, String reason, String fineractId) {
        log.warn("Handling activation failure: productId={} reason={}", productId, reason);
        errorMessage = reason;
        status = "FAILED";
        currentStep = "COMPENSATION";

        try {
            // SAGA Compensation: Deactivate Fineract product if it was created
            if (fineractId != null) {
                fineractActivity.deactivateLoanProduct(
                        new FineractProductActivity.DeactivateFineractProductInput(tenantId, fineractId));
            }

            // Revert product status to ACTIVATION_FAILED
            lifecycleActivity.markActivationFailed(
                    new ProductLifecycleActivity.MarkFailedInput(tenantId, productId, reason));

        } catch (Exception compensationError) {
            log.error("Compensation failed during product activation rollback: productId={}", productId, compensationError);
            errorMessage = reason + " (compensation also failed: " + compensationError.getMessage() + ")";
        }

        return new ActivationResult(productId, fineractId, "ACTIVATION_FAILED", reason, false);
    }

    @Override
    public ActivationStatus getStatus() {
        return new ActivationStatus(productId, currentStep, status, errorMessage, fineractProductId);
    }
}
