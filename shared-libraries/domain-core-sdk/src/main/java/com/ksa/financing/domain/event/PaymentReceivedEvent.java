package com.ksa.financing.domain.event;

import com.ksa.financing.domain.enums.PaymentMethod;
import com.ksa.financing.domain.valueobject.LoanId;
import com.ksa.financing.domain.valueobject.SarMoney;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a payment being received and recorded against a loan.
 * <p>
 * This event is published when a customer payment is successfully processed
 * and applied to reduce the outstanding loan balance.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record PaymentReceivedEvent(
        @NotNull UUID eventId,
        @NotNull LoanId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull SarMoney paymentAmount,
        @NotNull SarMoney principalPaid,
        @NotNull SarMoney profitPaid,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentMethod paymentMethod,
        String paymentReference,
        @NotNull SarMoney remainingPrincipal,
        @NotNull SarMoney remainingProfit
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public PaymentReceivedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(paymentAmount, "PaymentAmount cannot be null");
        Objects.requireNonNull(principalPaid, "PrincipalPaid cannot be null");
        Objects.requireNonNull(profitPaid, "ProfitPaid cannot be null");
        Objects.requireNonNull(paymentDate, "PaymentDate cannot be null");
        Objects.requireNonNull(paymentMethod, "PaymentMethod cannot be null");
        Objects.requireNonNull(remainingPrincipal, "RemainingPrincipal cannot be null");
        Objects.requireNonNull(remainingProfit, "RemainingProfit cannot be null");

        if (!paymentAmount.isPositive()) {
            throw new IllegalArgumentException("PaymentAmount must be positive");
        }

        if (principalPaid.isNegative() || profitPaid.isNegative()) {
            throw new IllegalArgumentException("Principal and profit paid cannot be negative");
        }

        if (remainingPrincipal.isNegative() || remainingProfit.isNegative()) {
            throw new IllegalArgumentException("Remaining balances cannot be negative");
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
