package com.ksa.financing.service.template.domain.port.out;

import com.ksa.financing.domain.base.DomainEvent;

/**
 * Output Port: Event publisher interface.
 * This is implemented by the infrastructure layer (Kafka adapter).
 *
 * Following Hexagonal Architecture:
 * - Domain publishes events through this interface
 * - Infrastructure decides how to publish (Kafka, RabbitMQ, etc.)
 */
public interface EventPublisher {

    /**
     * Publish a domain event.
     */
    void publish(DomainEvent event);

    /**
     * Publish multiple events in order.
     */
    void publishAll(Iterable<DomainEvent> events);
}