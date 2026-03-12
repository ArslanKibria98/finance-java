package com.ksa.islamic.orchestration.activity.product;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Step 4: Manages product status transitions and event publishing.
 * Handles: DRAFT → PENDING_ACTIVATION → ACTIVE (or ACTIVATION_FAILED).
 * Runs inside product-service.
 */
@ActivityInterface
public interface ProductLifecycleActivity {

    @ActivityMethod
    void updateStatusToPendingActivation(UpdateStatusInput input);

    @ActivityMethod
    void activateProduct(ActivateProductInput input);

    @ActivityMethod
    void markActivationFailed(MarkFailedInput input);

    @ActivityMethod
    void revertToDraft(RevertToDraftInput input);

    @ActivityMethod
    void storeFineractMapping(StoreMappingInput input);

    record UpdateStatusInput(
            String tenantId,
            String productId
    ) {}

    record ActivateProductInput(
            String tenantId,
            String productId,
            String fineractProductId
    ) {}

    record MarkFailedInput(
            String tenantId,
            String productId,
            String reason
    ) {}

    record RevertToDraftInput(
            String tenantId,
            String productId
    ) {}

    record StoreMappingInput(
            String tenantId,
            String productId,
            String productCode,
            String fineractProductId
    ) {}
}
