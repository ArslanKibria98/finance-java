package com.ksa.financing.service.template.adapter.temporal.activity;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ksa.financing.domain.model.TenantId;
import com.ksa.financing.service.template.domain.model.ExampleAggregateId;
import com.ksa.financing.service.template.domain.model.ExampleStatus;
import com.ksa.financing.service.template.domain.port.out.ExampleRepository;

/**
 * Implementation of ExampleActivity for Temporal workflows.
 * This is a driving adapter that can be called by Temporal workflows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExampleActivityImpl implements ExampleActivity {

    private final ExampleRepository repository;

    @Override
    public ProcessResult processExample(ProcessRequest request) {
        ActivityExecutionContext context = Activity.getExecutionContext();
        String workflowId = context.getInfo().getWorkflowId();
        String activityId = context.getInfo().getActivityId();

        log.info("Processing example. WorkflowId: {}, ActivityId: {}, AggregateId: {}",
                workflowId, activityId, request.aggregateId());

        try {
            // Load aggregate
            var tenantId = new TenantId(request.tenantId());
            var aggregateId = ExampleAggregateId.of(request.aggregateId());

            var aggregate = repository.findById(tenantId, aggregateId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aggregate not found: " + request.aggregateId()));

            // Simulate processing based on operation
            String resultStatus = switch (request.operation()) {
                case "APPROVE" -> {
                    if (aggregate.getStatus() == ExampleStatus.ACTIVE) {
                        yield "APPROVED";
                    } else {
                        yield "CANNOT_APPROVE";
                    }
                }
                case "REVIEW" -> "REVIEWED";
                case "ARCHIVE" -> "ARCHIVED";
                default -> "UNKNOWN_OPERATION";
            };

            log.info("Processed example successfully. Result: {}", resultStatus);

            return new ProcessResult(
                    request.aggregateId(),
                    resultStatus,
                    "Operation completed successfully"
            );

        } catch (Exception e) {
            log.error("Error processing example: {}", request.aggregateId(), e);
            throw Activity.wrap(e);
        }
    }

    @Override
    public ValidationResult validateExample(ValidationRequest request) {
        log.info("Validating example: {}", request.aggregateId());

        try {
            var tenantId = new TenantId(request.tenantId());
            var aggregateId = ExampleAggregateId.of(request.aggregateId());

            var aggregate = repository.findById(tenantId, aggregateId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aggregate not found: " + request.aggregateId()));

            // Perform validation logic
            boolean isValid = true;
            String reason = "Valid";

            if (aggregate.getStatus() == ExampleStatus.CANCELLED) {
                isValid = false;
                reason = "Aggregate is cancelled";
            } else if (aggregate.getEntities().isEmpty()) {
                isValid = false;
                reason = "Aggregate has no entities";
            }

            log.info("Validation result for {}: valid={}, reason={}",
                    request.aggregateId(), isValid, reason);

            return new ValidationResult(isValid, reason);

        } catch (Exception e) {
            log.error("Error validating example: {}", request.aggregateId(), e);
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }

    @Override
    public void sendNotification(NotificationRequest request) {
        log.info("Sending notification for example: {}", request.aggregateId());

        try {
            // In a real implementation, this would integrate with notification service
            // For now, just log the notification
            log.info("Notification sent: Type={}, Recipient={}, AggregateId={}",
                    request.type(), request.recipient(), request.aggregateId());

            // Simulate notification delay
            Thread.sleep(100);

        } catch (Exception e) {
            log.error("Error sending notification for example: {}", request.aggregateId(), e);
            throw Activity.wrap(e);
        }
    }
}