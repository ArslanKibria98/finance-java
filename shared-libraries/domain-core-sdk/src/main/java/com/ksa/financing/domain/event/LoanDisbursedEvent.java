package com.ksa.financing.domain.event;

import com.ksa.financing.domain.valueobject.CustomerId;
import com.ksa.financing.domain.valueobject.LoanId;
import com.ksa.financing.domain.valueobject.SarMoney;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a loan being disbursed to a customer.
 * <p>
 * This event is published when loan funds are transferred to the customer's
 * account and the loan becomes ACTIVE.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record LoanDisbursedEvent(
        @NotNull UUID eventId,
        @NotNull LoanId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull CustomerId customerId,
        @NotNull SarMoney disbursedAmount,
        @NotNull LocalDate disbursementDate,
        @NotNull LocalDate maturityDate,
        String disbursementMethod
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public LoanDisbursedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(customerId, "CustomerId cannot be null");
        Objects.requireNonNull(disbursedAmount, "DisbursedAmount cannot be null");
        Objects.requireNonNull(disbursementDate, "DisbursementDate cannot be null");
        Objects.requireNonNull(maturityDate, "MaturityDate cannot be null");

        if (!disbursedAmount.isPositive()) {
            throw new IllegalArgumentException("DisbursedAmount must be positive");
        }

        if (maturityDate.isBefore(disbursementDate)) {
            throw new IllegalArgumentException("MaturityDate cannot be before DisbursementDate");
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
