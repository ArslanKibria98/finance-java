package com.ksa.islamic.messaging.kafka.consumer;

import com.ksa.islamic.messaging.contract.EventEnvelope;
import com.ksa.islamic.messaging.exception.EventProcessingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.util.concurrent.TimeUnit;

/**
 * Base Domain Event Consumer with error handling
 * Abstract base class for all domain event consumers
 */
@Slf4j
public abstract class DomainEventConsumer<T> implements AcknowledgingMessageListener<String, EventEnvelope> {

    private final Class<T> eventType;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;

    protected DomainEventConsumer(Class<T> eventType, MeterRegistry meterRegistry) {
        this.eventType = eventType;

        String eventTypeName = eventType.getSimpleName().toLowerCase();

        this.successCounter = Counter.builder("domain.events.consumed.success")
            .tag("event.type", eventTypeName)
            .description("Number of successfully consumed domain events")
            .register(meterRegistry);

        this.errorCounter = Counter.builder("domain.events.consumed.error")
            .tag("event.type", eventTypeName)
            .description("Number of failed domain event consumptions")
            .register(meterRegistry);

        this.processingTimer = Timer.builder("domain.events.processing.duration")
            .tag("event.type", eventTypeName)
            .description("Duration of domain event processing")
            .register(meterRegistry);
    }

    @Override
    public void onMessage(ConsumerRecord<String, EventEnvelope> record, Acknowledgment acknowledgment) {
        long startTime = System.currentTimeMillis();
        EventEnvelope envelope = record.value();

        log.info("Received domain event: Topic={}, Partition={}, Offset={}, " +
                "EventType={}, EventId={}, TenantId={}, CorrelationId={}",
                record.topic(), record.partition(), record.offset(),
                envelope.getEventType(), envelope.getEventId(),
                envelope.getTenantId(), envelope.getCorrelationId());

        try {
            // Record processing duration
            processingTimer.record(() -> {
                // Validate event
                validateEvent(envelope);

                // Extract and cast payload
                T event = extractPayload(envelope);

                // Process the event
                processEvent(event, envelope);
            });

            // Acknowledge successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            successCounter.increment();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully processed event: EventId={}, Duration={}ms",
                    envelope.getEventId(), duration);

        } catch (Exception e) {
            errorCounter.increment();
            handleError(record, envelope, e, acknowledgment);
        }
    }

    /**
     * Process the domain event - to be implemented by concrete consumers
     *
     * @param event The typed event payload
     * @param envelope The event envelope containing metadata
     */
    protected abstract void processEvent(T event, EventEnvelope envelope) throws EventProcessingException;

    /**
     * Optional pre-processing hook
     */
    protected void preProcess(EventEnvelope envelope) {
        // Override in subclasses if needed
    }

    /**
     * Optional post-processing hook
     */
    protected void postProcess(EventEnvelope envelope) {
        // Override in subclasses if needed
    }

    /**
     * Validate the event envelope
     */
    protected void validateEvent(EventEnvelope envelope) throws EventProcessingException {
        if (envelope == null) {
            throw new EventProcessingException("Event envelope is null");
        }

        if (envelope.getEventId() == null) {
            throw new EventProcessingException("Event ID is null");
        }

        if (envelope.getTenantId() == null) {
            throw new EventProcessingException("Tenant ID is null");
        }

        if (envelope.getPayload() == null) {
            throw new EventProcessingException("Event payload is null");
        }

        // Additional validation can be added here
        validateEventType(envelope);
        validateEventVersion(envelope);
    }

    /**
     * Validate event type matches expected type
     */
    protected void validateEventType(EventEnvelope envelope) throws EventProcessingException {
        if (!eventType.getSimpleName().equals(envelope.getEventType())) {
            throw new EventProcessingException(
                String.format("Event type mismatch. Expected: %s, Actual: %s",
                             eventType.getSimpleName(), envelope.getEventType())
            );
        }
    }

    /**
     * Validate event version compatibility
     */
    protected void validateEventVersion(EventEnvelope envelope) throws EventProcessingException {
        // Override in subclasses for version-specific validation
        if (envelope.getEventVersion() == null) {
            log.warn("Event version is null for event: {}", envelope.getEventId());
        }
    }

    /**
     * Extract and cast the payload to the expected type
     */
    @SuppressWarnings("unchecked")
    protected T extractPayload(EventEnvelope envelope) throws EventProcessingException {
        try {
            Object payload = envelope.getPayload();

            if (eventType.isInstance(payload)) {
                return (T) payload;
            }

            // If payload is a Map, try to convert it
            if (payload instanceof java.util.Map) {
                // In production, use proper object mapper
                log.warn("Payload is a Map, conversion might be needed for: {}", eventType.getName());
                // This would need proper implementation with ObjectMapper
                throw new EventProcessingException("Map to object conversion not implemented");
            }

            throw new EventProcessingException(
                String.format("Cannot cast payload to %s", eventType.getName())
            );

        } catch (ClassCastException e) {
            throw new EventProcessingException("Failed to cast event payload", e);
        }
    }

    /**
     * Handle processing errors
     */
    protected void handleError(
            ConsumerRecord<String, EventEnvelope> record,
            EventEnvelope envelope,
            Exception exception,
            Acknowledgment acknowledgment) {

        log.error("Error processing event: Topic={}, Partition={}, Offset={}, " +
                 "EventId={}, Error={}",
                 record.topic(), record.partition(), record.offset(),
                 envelope != null ? envelope.getEventId() : "unknown",
                 exception.getMessage(), exception);

        // Determine if the error is recoverable
        if (isRecoverableError(exception)) {
            log.info("Error is recoverable, not acknowledging for retry");
            // Don't acknowledge - message will be retried
        } else {
            log.warn("Error is non-recoverable, acknowledging to avoid blocking");
            // Acknowledge to move past the problematic message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }

        // Report to monitoring system
        reportError(envelope, exception);
    }

    /**
     * Determine if an error is recoverable
     */
    protected boolean isRecoverableError(Exception exception) {
        // Non-recoverable errors
        if (exception instanceof IllegalArgumentException ||
            exception instanceof NullPointerException ||
            exception instanceof ClassCastException) {
            return false;
        }

        // Check for specific non-recoverable business exceptions
        if (exception instanceof EventProcessingException) {
            EventProcessingException epe = (EventProcessingException) exception;
            return epe.isRecoverable();
        }

        // Default to recoverable for unknown errors
        return true;
    }

    /**
     * Report error to monitoring/alerting system
     */
    protected void reportError(EventEnvelope envelope, Exception exception) {
        // Override in subclasses to implement specific error reporting
        // e.g., send to error tracking service, alert on-call, etc.
    }

    /**
     * Get the consumer group ID
     */
    protected String getConsumerGroupId() {
        return String.format("%s-consumer-group", eventType.getSimpleName().toLowerCase());
    }

    /**
     * Get the topic pattern for this consumer
     */
    protected String getTopicPattern() {
        return String.format("domain\\..*\\..*\\.%s", eventType.getSimpleName().toLowerCase());
    }
}