package com.ksa.islamic.messaging.contract;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Outbox Event entity for transactional outbox pattern
 * Ensures reliable event delivery with database-backed persistence
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the outbox event
     */
    @NonNull
    private String eventId;

    /**
     * Aggregate ID this event belongs to
     */
    @NonNull
    private String aggregateId;

    /**
     * Type of aggregate (e.g., "Loan", "Customer")
     */
    @NonNull
    private String aggregateType;

    /**
     * Type of event (e.g., "LoanApproved")
     */
    @NonNull
    private String eventType;

    /**
     * The event payload data
     */
    @NonNull
    private Object eventData;

    /**
     * Target Kafka topic
     */
    @NonNull
    private String topicName;

    /**
     * Partition key for Kafka (usually tenant ID)
     */
    private String partitionKey;

    /**
     * Tenant identifier
     */
    @NonNull
    private String tenantId;

    /**
     * Correlation ID for distributed tracing
     */
    private String correlationId;

    /**
     * Causation ID (parent event ID)
     */
    private String causationId;

    /**
     * When the event was created
     */
    @NonNull
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * When the event was processed/published
     */
    private Instant processedAt;

    /**
     * Number of retry attempts
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * Current status of the outbox event
     */
    @NonNull
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * Last error message if processing failed
     */
    private String lastError;

    /**
     * Additional metadata
     */
    private Map<String, String> metadata;

    /**
     * Scheduled time for processing (for delayed events)
     */
    private Instant scheduledFor;

    /**
     * User who triggered the event
     */
    private String userId;

    /**
     * Version for optimistic locking
     */
    private Long version;

    /**
     * Status enumeration for outbox events
     */
    public enum Status {
        /**
         * Event is pending processing
         */
        PENDING,

        /**
         * Event is currently being processed
         */
        PROCESSING,

        /**
         * Event has been successfully processed
         */
        PROCESSED,

        /**
         * Event processing failed after max retries
         */
        FAILED,

        /**
         * Event was cancelled/skipped
         */
        CANCELLED,

        /**
         * Event is scheduled for future processing
         */
        SCHEDULED
    }

    /**
     * Check if the event can be processed
     */
    public boolean canProcess() {
        if (status != Status.PENDING && status != Status.SCHEDULED) {
            return false;
        }

        if (status == Status.SCHEDULED) {
            return scheduledFor != null && Instant.now().isAfter(scheduledFor);
        }

        return true;
    }

    /**
     * Check if the event should be retried
     */
    public boolean shouldRetry(int maxRetries) {
        return status == Status.PENDING && retryCount < maxRetries;
    }

    /**
     * Mark as processing
     */
    public void markProcessing() {
        this.status = Status.PROCESSING;
    }

    /**
     * Mark as processed
     */
    public void markProcessed() {
        this.status = Status.PROCESSED;
        this.processedAt = Instant.now();
    }

    /**
     * Mark as failed
     */
    public void markFailed(String error) {
        this.lastError = error;
        if (retryCount >= 3) { // Default max retries
            this.status = Status.FAILED;
        }
    }

    /**
     * Increment retry count
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    /**
     * Create outbox event from domain event
     */
    public static OutboxEvent fromDomainEvent(
            Object event,
            String aggregateId,
            String aggregateType,
            String topicName,
            String tenantId) {

        return OutboxEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .eventType(event.getClass().getSimpleName())
            .eventData(event)
            .topicName(topicName)
            .partitionKey(tenantId)
            .tenantId(tenantId)
            .build();
    }

    /**
     * Schedule event for future processing
     */
    public OutboxEvent scheduleFor(Instant scheduledTime) {
        this.scheduledFor = scheduledTime;
        this.status = Status.SCHEDULED;
        return this;
    }

    /**
     * Get processing priority (lower retry count = higher priority)
     */
    public int getPriority() {
        // Failed events with fewer retries get higher priority
        if (status == Status.PENDING) {
            return 10 - Math.min(retryCount, 9);
        }
        return 0;
    }
}