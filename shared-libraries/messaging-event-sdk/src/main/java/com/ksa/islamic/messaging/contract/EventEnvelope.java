package com.ksa.islamic.messaging.contract;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event Envelope - Standard wrapper for all domain events
 * Provides consistent metadata structure for event-driven architecture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for this event instance
     */
    @NonNull
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * Type/name of the event (e.g., "LoanApproved", "CustomerRegistered")
     */
    @NonNull
    private String eventType;

    /**
     * Version of the event schema for evolution support
     */
    @NonNull
    @Builder.Default
    private String eventVersion = "1.0";

    /**
     * Tenant identifier for multi-tenancy
     */
    @NonNull
    private String tenantId;

    /**
     * Correlation ID for distributed tracing across services
     */
    @NonNull
    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    /**
     * ID of the event that caused this event (for event chains)
     */
    private String causationId;

    /**
     * Event creation timestamp
     */
    @NonNull
    @Builder.Default
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant timestamp = Instant.now();

    /**
     * Source service or system that generated the event
     */
    private String source;

    /**
     * User ID who triggered the event
     */
    private String userId;

    /**
     * The actual event payload
     */
    @NonNull
    private Object payload;

    /**
     * Additional metadata as key-value pairs
     */
    private Map<String, String> metadata;

    /**
     * Aggregate ID this event relates to
     */
    private String aggregateId;

    /**
     * Type of aggregate (e.g., "Loan", "Customer")
     */
    private String aggregateType;

    /**
     * Sequence number for ordering events within an aggregate
     */
    private Long sequenceNumber;

    /**
     * Create an event envelope with minimal required fields
     */
    public static EventEnvelope of(Object payload, String eventType, String tenantId) {
        return EventEnvelope.builder()
            .payload(payload)
            .eventType(eventType)
            .tenantId(tenantId)
            .build();
    }

    /**
     * Create an event envelope with correlation tracking
     */
    public static EventEnvelope withCorrelation(
            Object payload,
            String eventType,
            String tenantId,
            String correlationId) {
        return EventEnvelope.builder()
            .payload(payload)
            .eventType(eventType)
            .tenantId(tenantId)
            .correlationId(correlationId)
            .build();
    }

    /**
     * Create a causation chain event
     */
    public EventEnvelope causedBy(String parentEventId) {
        this.causationId = parentEventId;
        return this;
    }

    /**
     * Add metadata to the envelope
     */
    public EventEnvelope withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Set aggregate information
     */
    public EventEnvelope forAggregate(String aggregateId, String aggregateType) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        return this;
    }

    /**
     * Check if this is a system event
     */
    public boolean isSystemEvent() {
        return userId == null || userId.equals("SYSTEM");
    }

    /**
     * Get the topic name based on event type and aggregate
     */
    public String getTopicName() {
        if (aggregateType != null) {
            return String.format("domain.%s.%s.%s",
                extractContext(),
                aggregateType.toLowerCase(),
                eventType.toLowerCase());
        }
        return String.format("domain.events.%s", eventType.toLowerCase());
    }

    /**
     * Extract context from event type or aggregate type
     */
    private String extractContext() {
        // Simple heuristic - can be enhanced based on actual naming patterns
        if (eventType.contains("Loan") || eventType.contains("Lending")) {
            return "lending";
        } else if (eventType.contains("Customer")) {
            return "customer";
        } else if (eventType.contains("Payment")) {
            return "payment";
        }
        return "general";
    }

    /**
     * Validate the envelope has all required fields
     */
    public void validate() {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalStateException("Event ID is required");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalStateException("Event type is required");
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalStateException("Tenant ID is required");
        }
        if (payload == null) {
            throw new IllegalStateException("Event payload is required");
        }
    }
}