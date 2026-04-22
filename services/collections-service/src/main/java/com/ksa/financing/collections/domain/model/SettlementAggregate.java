package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for an early or partial settlement.
 * Tracks settlement lifecycle from quote/initiation to completion.
 */
public class SettlementAggregate {

    private final SettlementId id;
    private final UUID tenantId;
    private final String settlementNumber;
    private final UUID loanId;
    private final UUID customerId;
    private final SettlementType settlementType;

    private final BigDecimal settlementAmount;
    private final BigDecimal ibraAmount;
    private final BigDecimal discountAmount;
    
    private final LocalDate settlementDate;
    private final String idempotencyKey;

    private PaymentStatus status;
    private UUID paymentId;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    private final List<Object> uncommittedEvents = new ArrayList<>();

    private SettlementAggregate(SettlementId id, UUID tenantId, String settlementNumber,
                                UUID loanId, UUID customerId, SettlementType settlementType,
                                BigDecimal settlementAmount, BigDecimal ibraAmount,
                                BigDecimal discountAmount, LocalDate settlementDate,
                                String idempotencyKey) {
        this.id = id;
        this.tenantId = tenantId;
        this.settlementNumber = settlementNumber;
        this.loanId = loanId;
        this.customerId = customerId;
        this.settlementType = settlementType;
        this.settlementAmount = settlementAmount;
        this.ibraAmount = ibraAmount;
        this.discountAmount = discountAmount;
        this.settlementDate = settlementDate;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.version = 1;
    }

    public static SettlementAggregate initiate(UUID tenantId, String settlementNumber,
                                               UUID loanId, UUID customerId, SettlementType settlementType,
                                               BigDecimal settlementAmount, BigDecimal ibraAmount,
                                               BigDecimal discountAmount, LocalDate settlementDate,
                                               String idempotencyKey) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (loanId == null) throw new IllegalArgumentException("Loan ID cannot be null");
        if (settlementNumber == null || settlementNumber.isBlank())
            throw new IllegalArgumentException("Settlement number cannot be blank");
        if (settlementAmount == null || settlementAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Settlement amount cannot be negative");

        var settlement = new SettlementAggregate(
                SettlementId.generate(), tenantId, settlementNumber, loanId, customerId,
                settlementType, settlementAmount, ibraAmount, discountAmount,
                settlementDate, idempotencyKey);

        settlement.registerEvent(new SettlementInitiated(
                settlement.id, tenantId, loanId, customerId, settlementType,
                settlementAmount, ibraAmount, idempotencyKey));

        return settlement;
    }

    public static SettlementAggregate reconstitute(SettlementId id, UUID tenantId, String settlementNumber,
                                                    UUID loanId, UUID customerId, SettlementType settlementType,
                                                    BigDecimal settlementAmount, BigDecimal ibraAmount,
                                                    BigDecimal discountAmount, LocalDate settlementDate,
                                                    String idempotencyKey, PaymentStatus status, UUID paymentId,
                                                    LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        var settlement = new SettlementAggregate(id, tenantId, settlementNumber, loanId, customerId,
                settlementType, settlementAmount, ibraAmount, discountAmount, settlementDate, idempotencyKey);
        settlement.status = status;
        settlement.paymentId = paymentId;
        settlement.updatedAt = updatedAt;
        settlement.version = version;
        return settlement;
    }

    public void complete(UUID paymentId) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Can only complete a PENDING or PROCESSING settlement");
        }
        this.status = PaymentStatus.COMPLETED;
        this.paymentId = paymentId;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new SettlementConfirmed(id, tenantId, loanId, customerId, settlementAmount, ibraAmount, paymentId));
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    private void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    // Getters
    public SettlementId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getSettlementNumber() { return settlementNumber; }
    public UUID getLoanId() { return loanId; }
    public UUID getCustomerId() { return customerId; }
    public SettlementType getSettlementType() { return settlementType; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public BigDecimal getIbraAmount() { return ibraAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public PaymentStatus getStatus() { return status; }
    public UUID getPaymentId() { return paymentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // Domain Events
    public record SettlementInitiated(
            SettlementId settlementId, UUID tenantId, UUID loanId, UUID customerId,
            SettlementType settlementType, BigDecimal amount, BigDecimal ibraAmount,
            String idempotencyKey
    ) {}

    public record SettlementConfirmed(
            SettlementId settlementId, UUID tenantId, UUID loanId, UUID customerId,
            BigDecimal amount, BigDecimal ibraAmount, UUID paymentId
    ) {}
}
