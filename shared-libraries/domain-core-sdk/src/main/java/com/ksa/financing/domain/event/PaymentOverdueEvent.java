package com.ksa.financing.domain.event;

import com.ksa.financing.domain.enums.LoanStatus;
import com.ksa.financing.domain.valueobject.LoanId;
import com.ksa.financing.domain.valueobject.SarMoney;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a payment becoming overdue.
 * <p>
 * This event is published when a scheduled payment is not received by its
 * due date, and the loan moves into DELINQUENT or DEFAULT status based on
 * the days past due (DPD).
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record PaymentOverdueEvent(
        @NotNull UUID eventId,
        @NotNull LoanId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull LocalDate dueDate,
        @NotNull SarMoney overdueAmount,
        int daysPastDue,
        @NotNull LoanStatus newLoanStatus
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public PaymentOverdueEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(dueDate, "DueDate cannot be null");
        Objects.requireNonNull(overdueAmount, "OverdueAmount cannot be null");
        Objects.requireNonNull(newLoanStatus, "NewLoanStatus cannot be null");

        if (!overdueAmount.isPositive()) {
            throw new IllegalArgumentException("OverdueAmount must be positive");
        }

        if (daysPastDue < 0) {
            throw new IllegalArgumentException("DaysPastDue cannot be negative");
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
