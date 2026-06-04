package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** An IBFT transfer (wallet → external Canadian bank account via Scotia EFT), HOLD-based. */
public class IbftTransaction {
    private UUID id;
    private UUID tenantId;
    private String ibftNumber;
    private UUID customerId;
    private UUID walletId;
    private UUID beneficiaryId;
    private String debtorCorporateAccount;
    private String creditorAccount;
    private String creditorName;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private String currency;
    private IbftStatus status;
    private String purposeNote;
    private String endToEndId;
    private Long fineractHoldTxnId;
    private UUID holdMovementId;
    private UUID debitMovementId;
    private UUID releaseMovementId;
    private UUID ledgerEntryId;
    private String scotiaSubmissionId;
    private String scotiaPaymentId;
    private String scotiaStatus;
    private String idempotencyKey;
    private UUID initiatorUserId;
    private String initiatorIp;
    private String initiatorDeviceId;
    private int inquiryAttempts;
    private Instant lastInquiredAt;
    private String errorCode;
    private String errorMessage;
    private Instant initiatedAt;
    private Instant submittedAt;
    private Instant settledAt;
    private Instant failedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getIbftNumber() { return ibftNumber; }
    public void setIbftNumber(String ibftNumber) { this.ibftNumber = ibftNumber; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public UUID getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(UUID beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public String getDebtorCorporateAccount() { return debtorCorporateAccount; }
    public void setDebtorCorporateAccount(String v) { this.debtorCorporateAccount = v; }
    public String getCreditorAccount() { return creditorAccount; }
    public void setCreditorAccount(String creditorAccount) { this.creditorAccount = creditorAccount; }
    public String getCreditorName() { return creditorName; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public IbftStatus getStatus() { return status; }
    public void setStatus(IbftStatus status) { this.status = status; }
    public String getPurposeNote() { return purposeNote; }
    public void setPurposeNote(String purposeNote) { this.purposeNote = purposeNote; }
    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }
    public Long getFineractHoldTxnId() { return fineractHoldTxnId; }
    public void setFineractHoldTxnId(Long fineractHoldTxnId) { this.fineractHoldTxnId = fineractHoldTxnId; }
    public UUID getHoldMovementId() { return holdMovementId; }
    public void setHoldMovementId(UUID holdMovementId) { this.holdMovementId = holdMovementId; }
    public UUID getDebitMovementId() { return debitMovementId; }
    public void setDebitMovementId(UUID debitMovementId) { this.debitMovementId = debitMovementId; }
    public UUID getReleaseMovementId() { return releaseMovementId; }
    public void setReleaseMovementId(UUID releaseMovementId) { this.releaseMovementId = releaseMovementId; }
    public UUID getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(UUID ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }
    public String getScotiaSubmissionId() { return scotiaSubmissionId; }
    public void setScotiaSubmissionId(String scotiaSubmissionId) { this.scotiaSubmissionId = scotiaSubmissionId; }
    public String getScotiaPaymentId() { return scotiaPaymentId; }
    public void setScotiaPaymentId(String scotiaPaymentId) { this.scotiaPaymentId = scotiaPaymentId; }
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
    public int getInquiryAttempts() { return inquiryAttempts; }
    public void setInquiryAttempts(int inquiryAttempts) { this.inquiryAttempts = inquiryAttempts; }
    public Instant getLastInquiredAt() { return lastInquiredAt; }
    public void setLastInquiredAt(Instant lastInquiredAt) { this.lastInquiredAt = lastInquiredAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
