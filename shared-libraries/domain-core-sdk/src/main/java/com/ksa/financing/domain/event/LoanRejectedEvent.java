package com.ksa.financing.domain.event;

import com.ksa.financing.domain.valueobject.LoanId;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a loan application being rejected.
 * <p>
 * This event is published when a loan application is rejected by underwriting
 * or fails approval criteria. The loan status is moved to CLOSED.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record LoanRejectedEvent(
        @NotNull UUID eventId,
        @NotNull LoanId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull String rejectionReason,
        String rejectedBy
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public LoanRejectedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(rejectionReason, "RejectionReason cannot be null");

        if (rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("RejectionReason cannot be empty");
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
     * Get the loan ID (same as aggregateId).
     *
     * @return the loan ID
     */
    public LoanId getLoanId() {
        return aggregateId;
    }
}
