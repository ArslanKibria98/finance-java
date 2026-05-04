package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class WalletTransfer {
    private UUID id;
    private UUID tenantId;
    private String transferNumber;
    private UUID sourceWalletId;
    private UUID destinationWalletId;
    private UUID sourceCustomerId;
    private UUID destinationCustomerId;
    private TransferChannel channel;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal totalDebit;
    private String currency;
    private TransferStatus status;
    private String purposeNote;
    private UUID debitMovementId;
    private UUID creditMovementId;
    private UUID feeMovementId;
    private UUID ledgerEntryId;
    private String fineractTransferId;
    private String workflowId;
    private String idempotencyKey;
    private UUID initiatorUserId;
    private String initiatorIp;
    private String initiatorDeviceId;
    private String errorCode;
    private String errorMessage;
    private Instant initiatedAt;
    private Instant completedAt;
    private Instant reversedAt;
    private UUID reversalOfTransferId;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTransferNumber() { return transferNumber; }
    public void setTransferNumber(String transferNumber) { this.transferNumber = transferNumber; }
    public UUID getSourceWalletId() { return sourceWalletId; }
    public void setSourceWalletId(UUID sourceWalletId) { this.sourceWalletId = sourceWalletId; }
    public UUID getDestinationWalletId() { return destinationWalletId; }
    public void setDestinationWalletId(UUID destinationWalletId) { this.destinationWalletId = destinationWalletId; }
    public UUID getSourceCustomerId() { return sourceCustomerId; }
    public void setSourceCustomerId(UUID sourceCustomerId) { this.sourceCustomerId = sourceCustomerId; }
    public UUID getDestinationCustomerId() { return destinationCustomerId; }
    public void setDestinationCustomerId(UUID destinationCustomerId) { this.destinationCustomerId = destinationCustomerId; }
    public TransferChannel getChannel() { return channel; }
    public void setChannel(TransferChannel channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getPurposeNote() { return purposeNote; }
    public void setPurposeNote(String purposeNote) { this.purposeNote = purposeNote; }
    public UUID getDebitMovementId() { return debitMovementId; }
    public void setDebitMovementId(UUID debitMovementId) { this.debitMovementId = debitMovementId; }
    public UUID getCreditMovementId() { return creditMovementId; }
    public void setCreditMovementId(UUID creditMovementId) { this.creditMovementId = creditMovementId; }
    public UUID getFeeMovementId() { return feeMovementId; }
    public void setFeeMovementId(UUID feeMovementId) { this.feeMovementId = feeMovementId; }
    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }
    public String getFineractTransferId() { return fineractTransferId; }
    public void setFineractTransferId(String fineractTransferId) { this.fineractTransferId = fineractTransferId; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public UUID getInitiatorUserId() { return initiatorUserId; }
    public void setInitiatorUserId(UUID initiatorUserId) { this.initiatorUserId = initiatorUserId; }
    public String getInitiatorIp() { return initiatorIp; }
    public void setInitiatorIp(String initiatorIp) { this.initiatorIp = initiatorIp; }
    public String getInitiatorDeviceId() { return initiatorDeviceId; }
    public void setInitiatorDeviceId(String initiatorDeviceId) { this.initiatorDeviceId = initiatorDeviceId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }
    public UUID getReversalOfTransferId() { return reversalOfTransferId; }
    public void setReversalOfTransferId(UUID reversalOfTransferId) { this.reversalOfTransferId = reversalOfTransferId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
