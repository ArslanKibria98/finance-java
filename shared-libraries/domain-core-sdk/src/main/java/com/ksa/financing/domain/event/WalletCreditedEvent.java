package com.ksa.financing.domain.event;

import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.WalletId;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing a wallet being credited with funds.
 * <p>
 * This event is published when funds are added to a customer's wallet,
 * such as from loan disbursement, refunds, or deposits.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public record WalletCreditedEvent(
        @NotNull UUID eventId,
        @NotNull WalletId aggregateId,
        @NotNull LocalDateTime occurredOn,
        @NotNull SarMoney creditAmount,
        @NotNull String transactionReference,
        @NotNull SarMoney newBalance,
        String creditReason
) implements DomainEvent {

    /**
     * Canonical constructor with validation.
     */
    public WalletCreditedEvent {
        Objects.requireNonNull(eventId, "EventId cannot be null");
        Objects.requireNonNull(aggregateId, "AggregateId cannot be null");
        Objects.requireNonNull(occurredOn, "OccurredOn cannot be null");
        Objects.requireNonNull(creditAmount, "CreditAmount cannot be null");
        Objects.requireNonNull(transactionReference, "TransactionReference cannot be null");
        Objects.requireNonNull(newBalance, "NewBalance cannot be null");

        if (!creditAmount.isPositive()) {
            throw new IllegalArgumentException("CreditAmount must be positive");
        }

        if (transactionReference.trim().isEmpty()) {
            throw new IllegalArgumentException("TransactionReference cannot be empty");
        }

        if (newBalance.isNegative()) {
            throw new IllegalArgumentException("NewBalance cannot be negative");
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
     * Get the wallet ID (same as aggregateId).
     *
     * @return the wallet ID
     */
    public WalletId getWalletId() {
        return aggregateId;
    }
}
