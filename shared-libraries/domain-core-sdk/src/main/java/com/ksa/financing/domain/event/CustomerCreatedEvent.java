package com.ksa.financing.domain.event;

import com.ksa.financing.domain.enums.CustomerType;
import com.ksa.financing.domain.valueobject.CustomerId;
import com.ksa.financing.domain.valueobject.NationalId;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a new customer being created.
 * <p>
 * This event is published when a new customer record is created in the system,
 * either as an individual or business entity.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record CustomerCreatedEvent(
        @NotNull UUID eventId,
        @NotNull CustomerId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull CustomerType customerType,
        @NotNull NationalId nationalId,
        @NotNull String fullName,
        String emailAddress,
        String phoneNumber
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public CustomerCreatedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(customerType, "CustomerType cannot be null");
        Objects.requireNonNull(nationalId, "NationalId cannot be null");
        Objects.requireNonNull(fullName, "FullName cannot be null");

        if (fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("FullName cannot be empty");
        }
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    /**
     * Get the customer ID (same as aggregateId).
     *
     * @return the customer ID
     */
    public CustomerId getCustomerId() {
        return aggregateId;
    }
}
