package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An external bank transfer (to/from a Canadian bank account) executed via Scotia RTP.
 * Counterparty is an EXTERNAL account — there is no destination wallet.
 */
public class ExternalFundTransfer {
    private UUID id;
    private UUID tenantId;
    private String transferNumber;
    private ExternalTransferDirection direction;
    private UUID walletId;
    private UUID customerId;
    private String accountNumber;          // user's virtual account this leg moves through
    private String counterpartyName;
    private String counterpartyAccount;
    private String counterpartyEmail;
    private String counterpartyBankCode;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private String currency;
    private ExternalTransferStatus status;
    private String purposeNote;
    private UUID movementId;
    private UUID counterpartyWalletId;
    private UUID counterpartyMovementId;
    private boolean counterpartyInternal;
    private UUID ledgerEntryId;
    private String scotiaPaymentId;
    private String scotiaClearingRef;
    private String scotiaStatus;
    private String idempotencyKey;
    private UUID initiatorUserId;
    private String initiatorIp;
    private String initiatorDeviceId;
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
    public String getTransferNumber() { return transferNumber; }
    public void setTransferNumber(String transferNumber) { this.transferNumber = transferNumber; }
    public ExternalTransferDirection getDirection() { return direction; }
    public void setDirection(ExternalTransferDirection direction) { this.direction = direction; }
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
    public ExternalTransferStatus getStatus() { return status; }
    public void setStatus(ExternalTransferStatus status) { this.status = status; }
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
