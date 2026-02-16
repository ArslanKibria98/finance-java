package com.ksa.financing.service.template.adapter.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

/**
 * Temporal Activity Interface for example operations.
 * Activities are the building blocks of Temporal workflows.
 */
@ActivityInterface
public interface ExampleActivity {

    /**
     * Process an example aggregate.
     * This could be called from a workflow to perform business operations.
     */
    @ActivityMethod
    ProcessResult processExample(ProcessRequest request);

    /**
     * Validate an example aggregate.
     */
    @ActivityMethod
    ValidationResult validateExample(ValidationRequest request);

    /**
     * Send notification about an example.
     */
    @ActivityMethod
    void sendNotification(NotificationRequest request);

    // Request/Response DTOs
    record ProcessRequest(
            String tenantId,
            String aggregateId,
            String operation
    ) {}

    record ProcessResult(
            String aggregateId,
            String status,
            String message
    ) {}

    record ValidationRequest(
            String tenantId,
            String aggregateId
    ) {}

    record ValidationResult(
            boolean valid,
            String reason
    ) {}

    record NotificationRequest(
            String tenantId,
            String aggregateId,
            String type,
            String recipient
    ) {}

    /**
     * Default activity options for this activity.
     */
    static ActivityOptions defaultOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(2)
                        .setMaximumAttempts(3)
                        .build())
                .build();
    }
}