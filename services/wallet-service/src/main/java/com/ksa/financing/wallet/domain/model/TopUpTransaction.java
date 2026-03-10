package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TopUpTransaction {
    private UUID id;
    private UUID tenantId;
    private UUID walletId;
    private String transactionNumber;
    private TopUpMethod method;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String sourceCardLastFour;
    private String sourceCardBrand;
    private String sourceIban;
    private String providerTransactionId;
    private String providerReference;
    private TopUpStatus status;
    private UUID movementId;
    private String idempotencyKey;
    private String errorCode;
    private String errorMessage;
    private Instant initiatedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }
    public TopUpMethod getMethod() { return method; }
    public void setMethod(TopUpMethod method) { this.method = method; }
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
    public TopUpStatus getStatus() { return status; }
    public void setStatus(TopUpStatus status) { this.status = status; }
    public UUID getMovementId() { return movementId; }
    public void setMovementId(UUID movementId) { this.movementId = movementId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
