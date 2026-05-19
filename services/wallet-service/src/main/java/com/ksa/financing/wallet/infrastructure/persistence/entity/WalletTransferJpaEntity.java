package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transfers")
public class WalletTransferJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "transfer_number", nullable = false, length = 50)
    private String transferNumber;

    @Column(name = "source_wallet_id", nullable = false)
    private UUID sourceWalletId;

    @Column(name = "destination_wallet_id", nullable = false)
    private UUID destinationWalletId;

    @Column(name = "source_customer_id", nullable = false)
    private UUID sourceCustomerId;

    @Column(name = "destination_customer_id", nullable = false)
    private UUID destinationCustomerId;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "total_debit", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalDebit;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "SAR";

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "purpose_note", length = 280)
    private String purposeNote;

    @Column(name = "recipient_masked_name", length = 120)
    private String recipientMaskedName;

    @Column(name = "sender_masked_name", length = 120)
    private String senderMaskedName;

    @Column(name = "debit_movement_id")
    private UUID debitMovementId;

    @Column(name = "credit_movement_id")
    private UUID creditMovementId;

    @Column(name = "fee_movement_id")
    private UUID feeMovementId;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "fineract_transfer_id", length = 100)
    private String fineractTransferId;

    @Column(name = "workflow_id", length = 100)
    private String workflowId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "initiator_user_id")
    private UUID initiatorUserId;

    @Column(name = "initiator_ip", length = 45)
    private String initiatorIp;

    @Column(name = "initiator_device_id", length = 100)
    private String initiatorDeviceId;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "initiated_at", nullable = false)
    private OffsetDateTime initiatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "reversed_at")
    private OffsetDateTime reversedAt;

    @Column(name = "reversal_of_transfer_id")
    private UUID reversalOfTransferId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (initiatedAt == null) initiatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

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
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPurposeNote() { return purposeNote; }
    public void setPurposeNote(String purposeNote) { this.purposeNote = purposeNote; }
    public String getRecipientMaskedName() { return recipientMaskedName; }
    public void setRecipientMaskedName(String recipientMaskedName) { this.recipientMaskedName = recipientMaskedName; }
    public String getSenderMaskedName() { return senderMaskedName; }
    public void setSenderMaskedName(String senderMaskedName) { this.senderMaskedName = senderMaskedName; }
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
    public OffsetDateTime getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(OffsetDateTime initiatedAt) { this.initiatedAt = initiatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getReversedAt() { return reversedAt; }
    public void setReversedAt(OffsetDateTime reversedAt) { this.reversedAt = reversedAt; }
    public UUID getReversalOfTransferId() { return reversalOfTransferId; }
    public void setReversalOfTransferId(UUID reversalOfTransferId) { this.reversalOfTransferId = reversalOfTransferId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
