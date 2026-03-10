package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "top_up_transactions")
// NOTE: Filename should be TopUpTransactionJpaEntity.java - renamed class per naming conventions
public class TopUpTransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "transaction_number", nullable = false, length = 50)
    private String transactionNumber;

    @Column(name = "method", nullable = false, length = 20)
    private String method;

    @Column(name = "amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal netAmount;

    @Column(name = "source_card_last_four", length = 4)
    private String sourceCardLastFour;

    @Column(name = "source_card_brand", length = 20)
    private String sourceCardBrand;

    @Column(name = "source_iban", length = 34)
    private String sourceIban;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "movement_id")
    private UUID movementId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "initiated_at", nullable = false)
    private OffsetDateTime initiatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
        if (initiatedAt == null) {
            initiatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }

    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public String getSourceCardLastFour() { return sourceCardLastFour; }
    public void setSourceCardLastFour(String sourceCardLastFour) { this.sourceCardLastFour = sourceCardLastFour; }

    public String getSourceCardBrand() { return sourceCardBrand; }
    public void setSourceCardBrand(String sourceCardBrand) { this.sourceCardBrand = sourceCardBrand; }

    public String getSourceIban() { return sourceIban; }
    public void setSourceIban(String sourceIban) { this.sourceIban = sourceIban; }

    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }

    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getMovementId() { return movementId; }
    public void setMovementId(UUID movementId) { this.movementId = movementId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(OffsetDateTime initiatedAt) { this.initiatedAt = initiatedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
