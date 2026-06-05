package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ibft_transactions")
public class IbftTransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "ibft_number", nullable = false, length = 50) private String ibftNumber;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "wallet_id", nullable = false) private UUID walletId;
    @Column(name = "beneficiary_id", nullable = false) private UUID beneficiaryId;
    @Column(name = "debtor_corporate_account", length = 34) private String debtorCorporateAccount;
    @Column(name = "creditor_account", length = 40) private String creditorAccount;
    @Column(name = "creditor_name", length = 200) private String creditorName;
    @Column(name = "creditor_institution", length = 4) private String creditorInstitution;
    @Column(name = "creditor_transit", length = 5) private String creditorTransit;
    @Column(name = "creditor_account_no", length = 20) private String creditorAccountNo;
    @Column(name = "one_time", nullable = false) private boolean oneTime = false;
    @Column(name = "amount", nullable = false, precision = 20, scale = 6) private BigDecimal amount;
    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 6) private BigDecimal feeAmount = BigDecimal.ZERO;
    @Column(name = "currency", nullable = false, length = 3) private String currency = "CAD";
    @Column(name = "status", nullable = false, length = 20) private String status;
    @Column(name = "purpose_note", length = 280) private String purposeNote;
    @Column(name = "end_to_end_id", length = 40) private String endToEndId;
    @Column(name = "fineract_hold_txn_id") private Long fineractHoldTxnId;
    @Column(name = "hold_movement_id") private UUID holdMovementId;
    @Column(name = "debit_movement_id") private UUID debitMovementId;
    @Column(name = "release_movement_id") private UUID releaseMovementId;
    @Column(name = "ledger_entry_id") private UUID ledgerEntryId;
    @Column(name = "scotia_submission_id", length = 60) private String scotiaSubmissionId;
    @Column(name = "scotia_payment_id", length = 60) private String scotiaPaymentId;
    @Column(name = "scotia_status", length = 60) private String scotiaStatus;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @Column(name = "initiator_user_id") private UUID initiatorUserId;
    @Column(name = "initiator_ip", length = 45) private String initiatorIp;
    @Column(name = "initiator_device_id", length = 100) private String initiatorDeviceId;
    @Column(name = "inquiry_attempts", nullable = false) private int inquiryAttempts = 0;
    @Column(name = "last_inquired_at") private OffsetDateTime lastInquiredAt;
    @Column(name = "error_code", length = 80) private String errorCode;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "initiated_at", nullable = false) private OffsetDateTime initiatedAt;
    @Column(name = "submitted_at") private OffsetDateTime submittedAt;
    @Column(name = "settled_at") private OffsetDateTime settledAt;
    @Column(name = "failed_at") private OffsetDateTime failedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Version @Column(name = "version", nullable = false) private int version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (initiatedAt == null) initiatedAt = OffsetDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; } public void setTenantId(UUID v) { this.tenantId = v; }
    public String getIbftNumber() { return ibftNumber; } public void setIbftNumber(String v) { this.ibftNumber = v; }
    public UUID getCustomerId() { return customerId; } public void setCustomerId(UUID v) { this.customerId = v; }
    public UUID getWalletId() { return walletId; } public void setWalletId(UUID v) { this.walletId = v; }
    public UUID getBeneficiaryId() { return beneficiaryId; } public void setBeneficiaryId(UUID v) { this.beneficiaryId = v; }
    public String getDebtorCorporateAccount() { return debtorCorporateAccount; } public void setDebtorCorporateAccount(String v) { this.debtorCorporateAccount = v; }
    public String getCreditorAccount() { return creditorAccount; } public void setCreditorAccount(String v) { this.creditorAccount = v; }
    public String getCreditorName() { return creditorName; } public void setCreditorName(String v) { this.creditorName = v; }
    public String getCreditorInstitution() { return creditorInstitution; } public void setCreditorInstitution(String v) { this.creditorInstitution = v; }
    public String getCreditorTransit() { return creditorTransit; } public void setCreditorTransit(String v) { this.creditorTransit = v; }
    public String getCreditorAccountNo() { return creditorAccountNo; } public void setCreditorAccountNo(String v) { this.creditorAccountNo = v; }
    public boolean isOneTime() { return oneTime; } public void setOneTime(boolean v) { this.oneTime = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    public BigDecimal getFeeAmount() { return feeAmount; } public void setFeeAmount(BigDecimal v) { this.feeAmount = v; }
    public String getCurrency() { return currency; } public void setCurrency(String v) { this.currency = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public String getPurposeNote() { return purposeNote; } public void setPurposeNote(String v) { this.purposeNote = v; }
    public String getEndToEndId() { return endToEndId; } public void setEndToEndId(String v) { this.endToEndId = v; }
    public Long getFineractHoldTxnId() { return fineractHoldTxnId; } public void setFineractHoldTxnId(Long v) { this.fineractHoldTxnId = v; }
    public UUID getHoldMovementId() { return holdMovementId; } public void setHoldMovementId(UUID v) { this.holdMovementId = v; }
    public UUID getDebitMovementId() { return debitMovementId; } public void setDebitMovementId(UUID v) { this.debitMovementId = v; }
    public UUID getReleaseMovementId() { return releaseMovementId; } public void setReleaseMovementId(UUID v) { this.releaseMovementId = v; }
    public UUID getLedgerEntryId() { return ledgerEntryId; } public void setLedgerEntryId(UUID v) { this.ledgerEntryId = v; }
    public String getScotiaSubmissionId() { return scotiaSubmissionId; } public void setScotiaSubmissionId(String v) { this.scotiaSubmissionId = v; }
    public String getScotiaPaymentId() { return scotiaPaymentId; } public void setScotiaPaymentId(String v) { this.scotiaPaymentId = v; }
    public String getScotiaStatus() { return scotiaStatus; } public void setScotiaStatus(String v) { this.scotiaStatus = v; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
    public UUID getInitiatorUserId() { return initiatorUserId; } public void setInitiatorUserId(UUID v) { this.initiatorUserId = v; }
    public String getInitiatorIp() { return initiatorIp; } public void setInitiatorIp(String v) { this.initiatorIp = v; }
    public String getInitiatorDeviceId() { return initiatorDeviceId; } public void setInitiatorDeviceId(String v) { this.initiatorDeviceId = v; }
    public int getInquiryAttempts() { return inquiryAttempts; } public void setInquiryAttempts(int v) { this.inquiryAttempts = v; }
    public OffsetDateTime getLastInquiredAt() { return lastInquiredAt; } public void setLastInquiredAt(OffsetDateTime v) { this.lastInquiredAt = v; }
    public String getErrorCode() { return errorCode; } public void setErrorCode(String v) { this.errorCode = v; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String v) { this.errorMessage = v; }
    public OffsetDateTime getInitiatedAt() { return initiatedAt; } public void setInitiatedAt(OffsetDateTime v) { this.initiatedAt = v; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; } public void setSubmittedAt(OffsetDateTime v) { this.submittedAt = v; }
    public OffsetDateTime getSettledAt() { return settledAt; } public void setSettledAt(OffsetDateTime v) { this.settledAt = v; }
    public OffsetDateTime getFailedAt() { return failedAt; } public void setFailedAt(OffsetDateTime v) { this.failedAt = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(OffsetDateTime v) { this.updatedAt = v; }
    public int getVersion() { return version; } public void setVersion(int v) { this.version = v; }
}
