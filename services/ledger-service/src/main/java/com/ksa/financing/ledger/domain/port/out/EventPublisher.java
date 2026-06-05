package com.ksa.financing.ledger.domain.port.out;

import java.util.List;

/**
 * Output port: domain event publishing contract.
 * Implementations live in infrastructure.messaging.
 */
public interface EventPublisher {

    void publish(Object event);

    void publishAll(List<Object> events);
}
