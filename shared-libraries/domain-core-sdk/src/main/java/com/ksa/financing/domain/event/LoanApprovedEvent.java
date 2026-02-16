package com.ksa.financing.domain.event;

import com.ksa.financing.domain.valueobject.LoanId;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a loan being approved for disbursement.
 * <p>
 * This event is published when a loan application passes all approval
 * checks and is ready to be disbursed to the customer.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record LoanApprovedEvent(
        @NotNull UUID eventId,
        @NotNull LoanId aggregateId,
        @NotNull LocalDateTime occurredOn,
        String approvedBy,
        String approvalNotes
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public LoanApprovedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
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
