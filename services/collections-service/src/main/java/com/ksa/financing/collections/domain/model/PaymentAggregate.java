package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for a repayment payment transaction.
 * Tracks payment lifecycle from initiation to completion/failure.
 * Zero framework imports.
 */
public class PaymentAggregate {

    private final PaymentId id;
    private final UUID tenantId;
    private final String paymentNumber;
    private final UUID loanId;
    private final UUID installmentId;
    private final UUID customerId;
    private final String invoiceId;

    private final PaymentMethod paymentMethod;
    private final BigDecimal amount;
    private final String currency;

    private PaymentStatus status;
    private LocalDate valueDate;

    private UUID sourceWalletId;
    private String sourceReference;
    private String providerTransactionId;

    private final String idempotencyKey;

    private boolean ledgerSynced;
    private UUID ledgerEntryId;

    private String failureCode;
    private String failureMessage;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    private final List<Object> uncommittedEvents = new ArrayList<>();

    private PaymentAggregate(PaymentId id, UUID tenantId, String paymentNumber,
                              UUID loanId, UUID installmentId, UUID customerId,
                              String invoiceId, PaymentMethod paymentMethod, BigDecimal amount,
                              String currency, LocalDate valueDate,
                              String idempotencyKey) {
        this.id = id;
        this.tenantId = tenantId;
        this.paymentNumber = paymentNumber;
        this.loanId = loanId;
        this.installmentId = installmentId;
        this.customerId = customerId;
        this.invoiceId = invoiceId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.valueDate = valueDate;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
        this.ledgerSynced = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.version = 1;
    }

    // ==================== FACTORY METHOD ====================

    public static PaymentAggregate initiate(UUID tenantId, String paymentNumber,
                                             UUID loanId, UUID installmentId, UUID customerId,
                                             String invoiceId, PaymentMethod paymentMethod,
                                             BigDecimal amount, LocalDate valueDate,
                                             String idempotencyKey) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (loanId == null) throw new IllegalArgumentException("Loan ID cannot be null");
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        if (invoiceId == null || invoiceId.isBlank()) throw new IllegalArgumentException("Invoice ID cannot be blank");
        if (paymentNumber == null || paymentNumber.isBlank())
            throw new IllegalArgumentException("Payment number cannot be blank");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Payment amount must be positive");
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        if (paymentMethod == null)
            throw new IllegalArgumentException("Payment method cannot be null");

        var payment = new PaymentAggregate(
                PaymentId.generate(), tenantId, paymentNumber, loanId, installmentId, customerId,
                invoiceId, paymentMethod, amount, "SAR", valueDate, idempotencyKey);

        payment.registerEvent(new PaymentInitiated(
                payment.id, tenantId, loanId, installmentId, customerId, invoiceId, amount, paymentMethod, idempotencyKey));

        return payment;
    }

    public static PaymentAggregate reconstitute(PaymentId id, UUID tenantId,
                                                 String paymentNumber, UUID loanId, UUID installmentId,
                                                 UUID customerId, String invoiceId, PaymentMethod paymentMethod,
                                                 BigDecimal amount, String currency,
                                                 PaymentStatus status, LocalDate valueDate,
                                                 UUID sourceWalletId, String sourceReference,
                                                 String providerTransactionId,
                                                 String idempotencyKey, boolean ledgerSynced,
                                                 UUID ledgerEntryId, String failureCode,
                                                 String failureMessage,
                                                 LocalDateTime createdAt, LocalDateTime updatedAt,
                                                 int version) {
        var payment = new PaymentAggregate(id, tenantId, paymentNumber, loanId, installmentId, customerId,
                invoiceId, paymentMethod, amount, currency, valueDate, idempotencyKey);
        payment.status = status;
        payment.sourceWalletId = sourceWalletId;
        payment.sourceReference = sourceReference;
        payment.providerTransactionId = providerTransactionId;
        payment.ledgerSynced = ledgerSynced;
        payment.ledgerEntryId = ledgerEntryId;
        payment.failureCode = failureCode;
        payment.failureMessage = failureMessage;
        payment.updatedAt = updatedAt;
        payment.version = version;
        return payment;
    }

    // ==================== BUSINESS OPERATIONS ====================

    public void markProcessing() {
        if (status != PaymentStatus.PENDING)
            throw new IllegalStateException("Can only start processing a PENDING payment");
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String providerTransactionId) {
        if (status != PaymentStatus.PROCESSING && status != PaymentStatus.PENDING)
            throw new IllegalStateException("Can only complete a PENDING or PROCESSING payment");
        this.status = PaymentStatus.COMPLETED;
        this.providerTransactionId = providerTransactionId;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new PaymentCompleted(id, tenantId, loanId, customerId, amount, providerTransactionId));
    }

    public void fail(String failureCode, String failureMessage) {
        if (status == PaymentStatus.COMPLETED || status == PaymentStatus.REVERSED)
            throw new IllegalStateException("Cannot fail a completed or reversed payment");
        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new PaymentFailed(id, tenantId, loanId, amount, failureCode, failureMessage));
    }

    public void reverse(String reason) {
        if (status != PaymentStatus.COMPLETED)
            throw new IllegalStateException("Can only reverse a COMPLETED payment");
        this.status = PaymentStatus.REVERSED;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new PaymentReversed(id, tenantId, loanId, amount, reason));
    }

    public void markLedgerSynced(UUID ledgerEntryId) {
        this.ledgerSynced = true;
        this.ledgerEntryId = ledgerEntryId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setSourceWalletId(UUID sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    // ==================== EVENTS ====================

    private void registerEvent(Object event) { uncommittedEvents.add(event); }

    public List<Object> getUncommittedEvents() { return Collections.unmodifiableList(uncommittedEvents); }

    public void markEventsAsCommitted() { uncommittedEvents.clear(); }

    // ==================== GETTERS ====================

    public PaymentId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getPaymentNumber() { return paymentNumber; }
    public UUID getLoanId() { return loanId; }
    public UUID getInstallmentId() { return installmentId; }
    public UUID getCustomerId() { return customerId; }
    public String getInvoiceId() { return invoiceId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public LocalDate getValueDate() { return valueDate; }
    public UUID getSourceWalletId() { return sourceWalletId; }
    public String getSourceReference() { return sourceReference; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public boolean isLedgerSynced() { return ledgerSynced; }
    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    // ==================== DOMAIN EVENTS ====================

    public record PaymentInitiated(
            PaymentId paymentId, UUID tenantId, UUID loanId, UUID installmentId, UUID customerId,
            String invoiceId, BigDecimal amount, PaymentMethod paymentMethod, String idempotencyKey
    ) {}

    public record PaymentCompleted(
            PaymentId paymentId, UUID tenantId, UUID loanId, UUID customerId,
            BigDecimal amount, String providerTransactionId
    ) {}

    public record PaymentFailed(
            PaymentId paymentId, UUID tenantId, UUID loanId,
            BigDecimal amount, String failureCode, String failureMessage
    ) {}

    public record PaymentReversed(
            PaymentId paymentId, UUID tenantId, UUID loanId,
            BigDecimal amount, String reason
    ) {}
}
