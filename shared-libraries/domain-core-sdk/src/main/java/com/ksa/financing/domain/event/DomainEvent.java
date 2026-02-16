package com.ksa.financing.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for all domain events in the Islamic financing platform.
 * <p>
 * Domain events represent state changes in the domain that are significant
 * to the business and may trigger side effects in other parts of the system.
 * </p>
 * <p>
 * All domain events are immutable and contain the timestamp of when the
 * event occurred and a unique identifier.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface DomainEvent {

    /**
     * Get the unique identifier for this event.
     *
     * @return the event ID
     */
    UUID getEventId();

    /**
     * Get the timestamp when this event occurred.
     *
     * @return the event timestamp
     */
    LocalDateTime getOccurredOn();

    /**
     * Get the type name of this event.
     * By default, returns the simple class name.
     *
     * @return the event type name
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
}
