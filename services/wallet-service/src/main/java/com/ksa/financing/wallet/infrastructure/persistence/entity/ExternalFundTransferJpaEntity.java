package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_fund_transfers")
public class ExternalFundTransferJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "transfer_number", nullable = false, length = 50)
    private String transferNumber;

    @Column(name = "direction", nullable = false, length = 20)
    private String direction;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Column(name = "counterparty_name", length = 140)
    private String counterpartyName;

    @Column(name = "counterparty_account", length = 34)
    private String counterpartyAccount;

    @Column(name = "counterparty_email", length = 140)
    private String counterpartyEmail;

    @Column(name = "counterparty_bank_code", length = 10)
    private String counterpartyBankCode;

    @Column(name = "amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "CAD";

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "purpose_note", length = 280)
    private String purposeNote;

    @Column(name = "movement_id")
    private UUID movementId;

    @Column(name = "counterparty_wallet_id")
    private UUID counterpartyWalletId;

    @Column(name = "counterparty_movement_id")
    private UUID counterpartyMovementId;

    @Column(name = "counterparty_internal", nullable = false)
    private boolean counterpartyInternal = false;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "scotia_payment_id", length = 100)
    private String scotiaPaymentId;

    @Column(name = "scotia_clearing_ref", length = 100)
    private String scotiaClearingRef;

    @Column(name = "scotia_status", length = 60)
    private String scotiaStatus;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "initiator_user_id")
    private UUID initiatorUserId;

    @Column(name = "initiator_ip", length = 45)
    private String initiatorIp;

    @Column(name = "initiator_device_id", length = 100)
    private String initiatorDeviceId;

    @Column(name = "error_code", length = 80)
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
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }
    public String getCounterpartyEmail() { return counterpartyEmail; }
    public void setCounterpartyEmail(String counterpartyEmail) { this.counterpartyEmail = counterpartyEmail; }
    public String getCounterpartyBankCode() { return counterpartyBankCode; }
    public void setCounterpartyBankCode(String counterpartyBankCode) { this.counterpartyBankCode = counterpartyBankCode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPurposeNote() { return purposeNote; }
    public void setPurposeNote(String purposeNote) { this.purposeNote = purposeNote; }
    public UUID getMovementId() { return movementId; }
    public void setMovementId(UUID movementId) { this.movementId = movementId; }
    public UUID getCounterpartyWalletId() { return counterpartyWalletId; }
    public void setCounterpartyWalletId(UUID counterpartyWalletId) { this.counterpartyWalletId = counterpartyWalletId; }
    public UUID getCounterpartyMovementId() { return counterpartyMovementId; }
    public void setCounterpartyMovementId(UUID counterpartyMovementId) { this.counterpartyMovementId = counterpartyMovementId; }
    public boolean isCounterpartyInternal() { return counterpartyInternal; }
    public void setCounterpartyInternal(boolean counterpartyInternal) { this.counterpartyInternal = counterpartyInternal; }
    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }
    public String getScotiaPaymentId() { return scotiaPaymentId; }
    public void setScotiaPaymentId(String scotiaPaymentId) { this.scotiaPaymentId = scotiaPaymentId; }
    public String getScotiaClearingRef() { return scotiaClearingRef; }
    public void setScotiaClearingRef(String scotiaClearingRef) { this.scotiaClearingRef = scotiaClearingRef; }
    public String getScotiaStatus() { return scotiaStatus; }
    public void setScotiaStatus(String scotiaStatus) { this.scotiaStatus = scotiaStatus; }
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
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
