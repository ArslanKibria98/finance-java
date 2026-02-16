package com.ksa.financing.domain.model;

import com.ksa.financing.domain.event.DomainEvent;
import com.ksa.financing.domain.valueobject.CustomerId;
import com.ksa.financing.domain.valueobject.SarMoney;
import com.ksa.financing.domain.valueobject.WalletId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Wallet aggregate root representing a customer's wallet in the Islamic financing platform.
 * <p>
 * This aggregate manages:
 * - Wallet identity and owner
 * - Balance tracking
 * - Credit and debit operations
 * - Wallet status (active, frozen)
 * </p>
 * <p>
 * Business invariants enforced:
 * - Balance cannot go negative
 * - Frozen wallets cannot be debited or credited
 * - All transactions must have a reference
 * - Amounts must be positive
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public class Wallet {

    private final WalletId walletId;
    private final CustomerId customerId;

    private SarMoney balance;
    private String status;

    private final List<DomainEvent> domainEvents;

    /**
     * Private constructor for creating wallet instances.
     * Use factory methods instead.
     */
    private Wallet(WalletId walletId, CustomerId customerId, SarMoney balance, String status) {
        this.walletId = Objects.requireNonNull(walletId, "WalletId cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
        this.balance = Objects.requireNonNull(balance, "Balance cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.domainEvents = new ArrayList<>();

        validateInvariants();
    }

    /**
     * Factory method to create a new wallet for a customer.
     *
     * @param customerId the customer identifier
     * @return a new Wallet instance with zero balance
     */
    public static Wallet createWallet(CustomerId customerId) {
        Objects.requireNonNull(customerId, "CustomerId cannot be null");

        Wallet wallet = new Wallet(
                WalletId.generate(),
                customerId,
                SarMoney.zero(),
                "ACTIVE"
        );

        wallet.addDomainEvent(new WalletCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                wallet.walletId,
                wallet.customerId
        ));

        return wallet;
    }

    /**
     * Factory method to create a wallet with an initial balance.
     *
     * @param customerId the customer identifier
     * @param initialBalance the initial balance
     * @return a new Wallet instance
     */
    public static Wallet createWallet(CustomerId customerId, SarMoney initialBalance) {
        Objects.requireNonNull(customerId, "CustomerId cannot be null");
        Objects.requireNonNull(initialBalance, "InitialBalance cannot be null");

        if (initialBalance.isNegative()) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        Wallet wallet = new Wallet(
                WalletId.generate(),
                customerId,
                initialBalance,
                "ACTIVE"
        );

        wallet.addDomainEvent(new WalletCreatedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                wallet.walletId,
                wallet.customerId
        ));

        if (initialBalance.isPositive()) {
            wallet.addDomainEvent(new WalletCreditedEvent(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    wallet.walletId,
                    initialBalance,
                    "INITIAL_DEPOSIT",
                    wallet.balance
            ));
        }

        return wallet;
    }

    /**
     * Credit the wallet with an amount.
     *
     * @param amount the amount to credit
     * @param reference the transaction reference
     */
    public void credit(SarMoney amount, String reference) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(reference, "Reference cannot be null");

        if (reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference cannot be empty");
        }

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }

        if (!status.equals("ACTIVE")) {
            throw new IllegalStateException(
                    String.format("Cannot credit wallet with status %s", status)
            );
        }

        this.balance = this.balance.add(amount);

        addDomainEvent(new WalletCreditedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                walletId,
                amount,
                reference,
                balance
        ));
    }

    /**
     * Debit the wallet with an amount.
     * Balance cannot go negative.
     *
     * @param amount the amount to debit
     * @param reference the transaction reference
     */
    public void debit(SarMoney amount, String reference) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(reference, "Reference cannot be null");

        if (reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference cannot be empty");
        }

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        if (!status.equals("ACTIVE")) {
            throw new IllegalStateException(
                    String.format("Cannot debit wallet with status %s", status)
            );
        }

        // Check if balance would go negative
        SarMoney newBalance = this.balance.subtract(amount);
        if (newBalance.isNegative()) {
            throw new IllegalStateException(
                    String.format("Insufficient balance. Current: %s, Attempted debit: %s",
                            balance.toFormattedString(), amount.toFormattedString())
            );
        }

        this.balance = newBalance;

        addDomainEvent(new WalletDebitedEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                walletId,
                amount,
                reference,
                balance
        ));
    }

    /**
     * Freeze the wallet.
     * Frozen wallets cannot perform credit or debit operations.
     */
    public void freeze() {
        if (status.equals("FROZEN")) {
            // Already frozen, no-op
            return;
        }

        this.status = "FROZEN";

        addDomainEvent(new WalletFrozenEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                walletId
        ));
    }

    /**
     * Unfreeze the wallet.
     * Returns the wallet to ACTIVE status.
     */
    public void unfreeze() {
        if (status.equals("ACTIVE")) {
            // Already active, no-op
            return;
        }

        this.status = "ACTIVE";

        addDomainEvent(new WalletUnfrozenEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                walletId
        ));
    }

    /**
     * Check if the wallet has sufficient balance for a transaction.
     *
     * @param amount the amount to check
     * @return true if balance is sufficient, false otherwise
     */
    public boolean hasSufficientBalance(SarMoney amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        return balance.isGreaterThanOrEqualTo(amount);
    }

    /**
     * Check if the wallet is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return status.equals("ACTIVE");
    }

    /**
     * Check if the wallet is frozen.
     *
     * @return true if frozen, false otherwise
     */
    public boolean isFrozen() {
        return status.equals("FROZEN");
    }

    /**
     * Validate business invariants.
     */
    private void validateInvariants() {
        if (balance.isNegative()) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
    }

    /**
     * Add a domain event to the list.
     *
     * @param event the domain event
     */
    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * Get all domain events.
     *
     * @return unmodifiable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all domain events.
     * Should be called after events have been published.
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Getters

    public WalletId getWalletId() {
        return walletId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public SarMoney getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    // Domain Event Classes (inner classes for simplicity)

    public record WalletCreatedEvent(UUID eventId, LocalDateTime occurredOn, WalletId walletId,
                                      CustomerId customerId)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record WalletCreditedEvent(UUID eventId, LocalDateTime occurredOn, WalletId walletId,
                                       SarMoney amount, String reference, SarMoney newBalance)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record WalletDebitedEvent(UUID eventId, LocalDateTime occurredOn, WalletId walletId,
                                      SarMoney amount, String reference, SarMoney newBalance)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record WalletFrozenEvent(UUID eventId, LocalDateTime occurredOn, WalletId walletId)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }

    public record WalletUnfrozenEvent(UUID eventId, LocalDateTime occurredOn, WalletId walletId)
            implements DomainEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }
}
