package com.ksa.islamic.reporting.projector;

import com.ksa.financing.domain.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for events that can be projected into read models.
 * Extends DomainEvent with additional fields needed for projection.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseProjectableEvent implements DomainEvent {

    private UUID eventId;
    private LocalDateTime occurredOn;
    private String aggregateId;
    private String aggregateType;
    private String tenantId;
    private Long version;

    @Override
    public UUID getEventId() {
        return eventId != null ? eventId : UUID.randomUUID();
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn != null ? occurredOn : LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}