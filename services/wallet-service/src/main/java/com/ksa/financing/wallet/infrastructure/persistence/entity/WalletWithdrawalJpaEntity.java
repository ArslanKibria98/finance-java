package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_withdrawals")
public class WalletWithdrawalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "withdrawal_number", nullable = false, length = 50)
    private String withdrawalNumber;

    @Column(name = "source_wallet_id", nullable = false)
    private UUID sourceWalletId;

    @Column(name = "source_customer_id", nullable = false)
    private UUID sourceCustomerId;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "destination_iban", nullable = false, length = 34)
    private String destinationIban;

    @Column(name = "destination_bank_code", length = 20)
    private String destinationBankCode;

    @Column(name = "destination_bank_name", length = 120)
    private String destinationBankName;

    @Column(name = "beneficiary_name", nullable = false, length = 200)
    private String beneficiaryName;

    @Column(name = "beneficiary_id")
    private UUID beneficiaryId;

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

    @Column(name = "purpose_code", length = 4)
    private String purposeCode;

    @Column(name = "charge_bearer", nullable = false, length = 4)
    private String chargeBearer = "DEBT";

    @Column(name = "service_level", nullable = false, length = 4)
    private String serviceLevel = "NURG";

    @Column(name = "end_to_end_id", length = 35)
    private String endToEndId;

    @Column(name = "uetr")
    private UUID uetr;

    @Column(name = "instruction_id", length = 35)
    private String instructionId;

    @Column(name = "destination_country", length = 2)
    private String destinationCountry;

    // ─── AML screening ──────────────────────────────────────────────
    @Column(name = "screening_ref", length = 50)
    private String screeningRef;

    @Column(name = "screening_decision", length = 20)
    private String screeningDecision;

    @Column(name = "screening_score")
    private Integer screeningScore;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.OTHER)
    @Column(name = "screening_matches", columnDefinition = "jsonb")
    private String screeningMatches;

    @Column(name = "screened_at")
    private OffsetDateTime screenedAt;

    @Column(name = "edd_required", nullable = false)
    private boolean eddRequired = false;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "release_reason", columnDefinition = "TEXT")
    private String releaseReason;

    @Column(name = "debit_movement_id")
    private UUID debitMovementId;

    @Column(name = "fee_movement_id")
    private UUID feeMovementId;

    @Column(name = "refund_movement_id")
    private UUID refundMovementId;

    @Column(name = "fineract_debit_txn_id", length = 100)
    private String fineractDebitTxnId;

    @Column(name = "fineract_refund_txn_id", length = 100)
    private String fineractRefundTxnId;

    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "sarie_reference", length = 100)
    private String sarieReference;

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

    @Column(name = "debited_at")
    private OffsetDateTime debitedAt;

    @Column(name = "bank_submitted_at")
    private OffsetDateTime bankSubmittedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "compensated_at")
    private OffsetDateTime compensatedAt;

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
    public String getWithdrawalNumber() { return withdrawalNumber; }
    public void setWithdrawalNumber(String withdrawalNumber) { this.withdrawalNumber = withdrawalNumber; }
    public UUID getSourceWalletId() { return sourceWalletId; }
    public void setSourceWalletId(UUID sourceWalletId) { this.sourceWalletId = sourceWalletId; }
    public UUID getSourceCustomerId() { return sourceCustomerId; }
    public void setSourceCustomerId(UUID sourceCustomerId) { this.sourceCustomerId = sourceCustomerId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getDestinationIban() { return destinationIban; }
    public void setDestinationIban(String destinationIban) { this.destinationIban = destinationIban; }
    public String getDestinationBankCode() { return destinationBankCode; }
    public void setDestinationBankCode(String destinationBankCode) { this.destinationBankCode = destinationBankCode; }
    public String getDestinationBankName() { return destinationBankName; }
    public void setDestinationBankName(String destinationBankName) { this.destinationBankName = destinationBankName; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public UUID getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(UUID beneficiaryId) { this.beneficiaryId = beneficiaryId; }
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
    public String getPurposeCode() { return purposeCode; }
    public void setPurposeCode(String purposeCode) { this.purposeCode = purposeCode; }
    public String getChargeBearer() { return chargeBearer; }
    public void setChargeBearer(String chargeBearer) { this.chargeBearer = chargeBearer; }
    public String getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }
    public UUID getUetr() { return uetr; }
    public void setUetr(UUID uetr) { this.uetr = uetr; }
    public String getInstructionId() { return instructionId; }
    public void setInstructionId(String instructionId) { this.instructionId = instructionId; }
    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }
    public String getScreeningRef() { return screeningRef; }
    public void setScreeningRef(String screeningRef) { this.screeningRef = screeningRef; }
    public String getScreeningDecision() { return screeningDecision; }
    public void setScreeningDecision(String screeningDecision) { this.screeningDecision = screeningDecision; }
    public Integer getScreeningScore() { return screeningScore; }
    public void setScreeningScore(Integer screeningScore) { this.screeningScore = screeningScore; }
    public String getScreeningMatches() { return screeningMatches; }
    public void setScreeningMatches(String screeningMatches) { this.screeningMatches = screeningMatches; }
    public OffsetDateTime getScreenedAt() { return screenedAt; }
    public void setScreenedAt(OffsetDateTime screenedAt) { this.screenedAt = screenedAt; }
    public boolean isEddRequired() { return eddRequired; }
    public void setEddRequired(boolean eddRequired) { this.eddRequired = eddRequired; }
    public UUID getReleasedBy() { return releasedBy; }
    public void setReleasedBy(UUID releasedBy) { this.releasedBy = releasedBy; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(OffsetDateTime releasedAt) { this.releasedAt = releasedAt; }
    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String releaseReason) { this.releaseReason = releaseReason; }
    public UUID getDebitMovementId() { return debitMovementId; }
    public void setDebitMovementId(UUID debitMovementId) { this.debitMovementId = debitMovementId; }
    public UUID getFeeMovementId() { return feeMovementId; }
    public void setFeeMovementId(UUID feeMovementId) { this.feeMovementId = feeMovementId; }
    public UUID getRefundMovementId() { return refundMovementId; }
    public void setRefundMovementId(UUID refundMovementId) { this.refundMovementId = refundMovementId; }
    public String getFineractDebitTxnId() { return fineractDebitTxnId; }
    public void setFineractDebitTxnId(String fineractDebitTxnId) { this.fineractDebitTxnId = fineractDebitTxnId; }
    public String getFineractRefundTxnId() { return fineractRefundTxnId; }
    public void setFineractRefundTxnId(String fineractRefundTxnId) { this.fineractRefundTxnId = fineractRefundTxnId; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    public String getSarieReference() { return sarieReference; }
    public void setSarieReference(String sarieReference) { this.sarieReference = sarieReference; }
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
    public OffsetDateTime getDebitedAt() { return debitedAt; }
    public void setDebitedAt(OffsetDateTime debitedAt) { this.debitedAt = debitedAt; }
    public OffsetDateTime getBankSubmittedAt() { return bankSubmittedAt; }
    public void setBankSubmittedAt(OffsetDateTime bankSubmittedAt) { this.bankSubmittedAt = bankSubmittedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(OffsetDateTime failedAt) { this.failedAt = failedAt; }
    public OffsetDateTime getCompensatedAt() { return compensatedAt; }
    public void setCompensatedAt(OffsetDateTime compensatedAt) { this.compensatedAt = compensatedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
