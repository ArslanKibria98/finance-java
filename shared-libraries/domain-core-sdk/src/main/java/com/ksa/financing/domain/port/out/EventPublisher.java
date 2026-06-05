package com.ksa.financing.domain.port.out;

import com.ksa.financing.domain.event.DomainEvent;

import java.util.List;

/**
 * Output port (SPI) for publishing domain events.
 * <p>
 * This interface defines the contract for event publisher implementations.
 * It enables the domain layer to publish events without depending on
 * specific messaging infrastructure (e.g., Kafka, RabbitMQ, AWS SNS).
 * </p>
 * <p>
 * Implementation guidelines:
 * - Must guarantee at-least-once delivery of events
 * - Should implement outbox pattern for transactional messaging
 * - Should support both synchronous and asynchronous publication
 * - Must handle serialization to appropriate event format
 * - Should include event metadata (correlation ID, causation ID, etc.)
 * </p>
 * <p>
 * Events are typically published after successful persistence of aggregates
 * to maintain consistency between domain state and published events.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface EventPublisher {

    /**
     * Publish a single domain event.
     * <p>
     * The event will be serialized and sent to the appropriate topic/queue
     * based on the event type. Implementations should ensure the event is
     * durably stored before returning (outbox pattern).
     * </p>
     *
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);

    /**
     * Publish multiple domain events.
     * <p>
     * All events are published atomically as part of the same transaction.
     * This is useful for publishing multiple events from a single aggregate
     * or from multiple aggregates within a single use case.
     * </p>
     *
     * @param events the list of domain events to publish
     */
    void publish(List<DomainEvent> events);
}
