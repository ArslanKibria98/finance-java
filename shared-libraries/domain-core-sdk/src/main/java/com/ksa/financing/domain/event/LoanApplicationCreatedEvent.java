package com.ksa.financing.domain.event;

import com.ksa.financing.domain.valueobject.CustomerId;
import com.ksa.financing.domain.valueobject.LoanId;
import com.ksa.financing.domain.valueobject.ProductId;
import com.ksa.financing.domain.valueobject.SarMoney;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a loan application being created.
 * <p>
 * This event is published when a customer submits a new loan application
 * and the loan aggregate is created in PENDING_DISBURSEMENT status.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record LoanApplicationCreatedEvent(
        @NotNull UUID eventId,
        @NotNull LoanId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull CustomerId customerId,
        @NotNull ProductId productId,
        @NotNull SarMoney requestedAmount,
        @NotNull SarMoney totalAmount,
        int tenureMonths
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public LoanApplicationCreatedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(customerId, "CustomerId cannot be null");
        Objects.requireNonNull(productId, "ProductId cannot be null");
        Objects.requireNonNull(requestedAmount, "RequestedAmount cannot be null");
        Objects.requireNonNull(totalAmount, "TotalAmount cannot be null");

        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("TenureMonths must be positive");
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
