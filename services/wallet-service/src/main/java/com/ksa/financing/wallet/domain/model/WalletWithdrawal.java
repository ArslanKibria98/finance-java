package com.ksa.financing.wallet.domain.model;

import com.ksa.financing.wallet.domain.iso.ChargeBearerType1Code;
import com.ksa.financing.wallet.domain.iso.ExternalPurpose1Code;
import com.ksa.financing.wallet.domain.iso.ServiceLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate representing an outbound withdrawal (cash-out) from a wallet
 * to an external bank IBAN.
 * <p>
 * Lifecycle (see {@link WithdrawalStatus}):
 *   PENDING → VALIDATED → DEBITED → BANK_SUBMITTED → COMPLETED
 *                                  ↘ COMPENSATED (refund posted)
 *                  ↘ FAILED (no debit yet — no compensation)
 *                  ↘ CANCELLED
 * <p>
 * Audit only — actual money lives in Fineract via ledger-service proxy.
 */
public class WalletWithdrawal {
    private UUID id;
    private UUID tenantId;
    private String withdrawalNumber;
    private UUID sourceWalletId;
    private UUID sourceCustomerId;
    private WithdrawalChannel channel;
    // Destination snapshot
    private String destinationIban;
    private String destinationBankCode;
    private String destinationBankName;
    private String beneficiaryName;
    private UUID beneficiaryId;
    // Money
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal totalDebit;
    private String currency;
    // State
    private WithdrawalStatus status;
    private String purposeNote;
    private ExternalPurpose1Code purposeCode;
    // ISO 20022 fields
    private ChargeBearerType1Code chargeBearer;
    private ServiceLevel serviceLevel;
    private String endToEndId;
    private UUID uetr;
    private String instructionId;
    private String destinationCountry;
    // AML screening
    private String screeningRef;
    private String screeningDecision;
    private Integer screeningScore;
    private String screeningMatchesJson;
    private Instant screenedAt;
    private boolean eddRequired;
    private UUID releasedBy;
    private Instant releasedAt;
    private String releaseReason;
    // Refs
    private UUID debitMovementId;
    private UUID feeMovementId;
    private UUID refundMovementId;
    private String fineractDebitTxnId;
    private String fineractRefundTxnId;
    private String bankReference;
    private String sarieReference;
    private String workflowId;
    // Idempotency / audit
    private String idempotencyKey;
    private UUID initiatorUserId;
    private String initiatorIp;
    private String initiatorDeviceId;
    // Errors
    private String errorCode;
    private String errorMessage;
    // Timestamps
    private Instant initiatedAt;
    private Instant debitedAt;
    private Instant bankSubmittedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant compensatedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

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
    public WithdrawalChannel getChannel() { return channel; }
    public void setChannel(WithdrawalChannel channel) { this.channel = channel; }
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
    public WithdrawalStatus getStatus() { return status; }
    public void setStatus(WithdrawalStatus status) { this.status = status; }
    public String getPurposeNote() { return purposeNote; }
    public void setPurposeNote(String purposeNote) { this.purposeNote = purposeNote; }
    public ExternalPurpose1Code getPurposeCode() { return purposeCode; }
    public void setPurposeCode(ExternalPurpose1Code purposeCode) { this.purposeCode = purposeCode; }
    public ChargeBearerType1Code getChargeBearer() { return chargeBearer; }
    public void setChargeBearer(ChargeBearerType1Code chargeBearer) { this.chargeBearer = chargeBearer; }
    public ServiceLevel getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(ServiceLevel serviceLevel) { this.serviceLevel = serviceLevel; }
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
    public String getScreeningMatchesJson() { return screeningMatchesJson; }
    public void setScreeningMatchesJson(String screeningMatchesJson) { this.screeningMatchesJson = screeningMatchesJson; }
    public Instant getScreenedAt() { return screenedAt; }
    public void setScreenedAt(Instant screenedAt) { this.screenedAt = screenedAt; }
    public boolean isEddRequired() { return eddRequired; }
    public void setEddRequired(boolean eddRequired) { this.eddRequired = eddRequired; }
    public UUID getReleasedBy() { return releasedBy; }
    public void setReleasedBy(UUID releasedBy) { this.releasedBy = releasedBy; }
    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
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
    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }
    public Instant getDebitedAt() { return debitedAt; }
    public void setDebitedAt(Instant debitedAt) { this.debitedAt = debitedAt; }
    public Instant getBankSubmittedAt() { return bankSubmittedAt; }
    public void setBankSubmittedAt(Instant bankSubmittedAt) { this.bankSubmittedAt = bankSubmittedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
    public Instant getCompensatedAt() { return compensatedAt; }
    public void setCompensatedAt(Instant compensatedAt) { this.compensatedAt = compensatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
