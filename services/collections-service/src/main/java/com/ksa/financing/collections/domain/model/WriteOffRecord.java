package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit aggregate for a single write-off transaction.
 * One record per installment (or per loan when cascading a full-loan write-off).
 * Immutable after creation — reversals create a new RECOVERY_REVERSAL record and
 * flip the original's status to REVERSED via {@link #reverse(String, UUID)}.
 */
public class WriteOffRecord {

    private final WriteOffId id;
    private final UUID tenantId;
    private final UUID loanId;
    private final UUID scheduleId;
    private final UUID installmentId;
    private final UUID delinquencyRuleId;

    private final BigDecimal principalAmount;
    private final BigDecimal profitAmount;
    private final BigDecimal feeAmount;
    private final BigDecimal penaltyAmount;
    private final BigDecimal totalAmount;
    private final int dpdAtWriteOff;

    private final WriteOffTriggerType triggerType;
    private final String reason;
    private final String approvalReference;

    private WriteOffStatus status;
    private String reversalReason;
    private LocalDateTime reversedAt;
    private UUID reversedBy;

    private final LocalDate writeOffDate;
    private final UUID initiatedBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private WriteOffRecord(WriteOffId id, UUID tenantId, UUID loanId, UUID scheduleId,
                           UUID installmentId, UUID delinquencyRuleId,
                           BigDecimal principalAmount, BigDecimal profitAmount,
                           BigDecimal feeAmount, BigDecimal penaltyAmount,
                           int dpdAtWriteOff,
                           WriteOffTriggerType triggerType, String reason, String approvalReference,
                           LocalDate writeOffDate, UUID initiatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.loanId = loanId;
        this.scheduleId = scheduleId;
        this.installmentId = installmentId;
        this.delinquencyRuleId = delinquencyRuleId;
        this.principalAmount = nz(principalAmount);
        this.profitAmount = nz(profitAmount);
        this.feeAmount = nz(feeAmount);
        this.penaltyAmount = nz(penaltyAmount);
        this.totalAmount = this.principalAmount.add(this.profitAmount)
                .add(this.feeAmount).add(this.penaltyAmount);
        this.dpdAtWriteOff = dpdAtWriteOff;
        this.triggerType = triggerType != null ? triggerType : WriteOffTriggerType.MANUAL;
        this.reason = reason;
        this.approvalReference = approvalReference;
        this.status = WriteOffStatus.ACTIVE;
        this.writeOffDate = writeOffDate != null ? writeOffDate : LocalDate.now();
        this.initiatedBy = initiatedBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static WriteOffRecord create(UUID tenantId, UUID loanId, UUID scheduleId,
                                        UUID installmentId, UUID delinquencyRuleId,
                                        BigDecimal principal, BigDecimal profit,
                                        BigDecimal fee, BigDecimal penalty,
                                        int dpd, WriteOffTriggerType trigger,
                                        String reason, String approvalReference,
                                        LocalDate writeOffDate, UUID initiatedBy) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (loanId == null) throw new IllegalArgumentException("loanId required");
        if (initiatedBy == null) throw new IllegalArgumentException("initiatedBy required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason required");

        BigDecimal total = nz(principal).add(nz(profit)).add(nz(fee)).add(nz(penalty));
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Write-off total must be positive");
        }

        return new WriteOffRecord(WriteOffId.generate(), tenantId, loanId, scheduleId,
                installmentId, delinquencyRuleId, principal, profit, fee, penalty,
                dpd, trigger, reason, approvalReference, writeOffDate, initiatedBy);
    }

    public static WriteOffRecord hydrate(WriteOffId id, UUID tenantId, UUID loanId, UUID scheduleId,
                                         UUID installmentId, UUID delinquencyRuleId,
                                         BigDecimal principal, BigDecimal profit,
                                         BigDecimal fee, BigDecimal penalty,
                                         int dpd, WriteOffTriggerType trigger, String reason,
                                         String approvalReference, WriteOffStatus status,
                                         String reversalReason, LocalDateTime reversedAt, UUID reversedBy,
                                         LocalDate writeOffDate, UUID initiatedBy,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        var rec = new WriteOffRecord(id, tenantId, loanId, scheduleId, installmentId,
                delinquencyRuleId, principal, profit, fee, penalty, dpd, trigger,
                reason, approvalReference, writeOffDate, initiatedBy);
        rec.status = status != null ? status : WriteOffStatus.ACTIVE;
        rec.reversalReason = reversalReason;
        rec.reversedAt = reversedAt;
        rec.reversedBy = reversedBy;
        rec.updatedAt = updatedAt != null ? updatedAt : createdAt;
        return rec;
    }

    public void reverse(String reason, UUID actorId) {
        if (this.status == WriteOffStatus.REVERSED) {
            throw new IllegalStateException("Write-off already reversed");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reversal reason required");
        }
        if (actorId == null) {
            throw new IllegalArgumentException("Actor id required");
        }
        this.status = WriteOffStatus.REVERSED;
        this.reversalReason = reason;
        this.reversedBy = actorId;
        this.reversedAt = LocalDateTime.now();
        this.updatedAt = this.reversedAt;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // Getters
    public WriteOffId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getLoanId() { return loanId; }
    public UUID getScheduleId() { return scheduleId; }
    public UUID getInstallmentId() { return installmentId; }
    public UUID getDelinquencyRuleId() { return delinquencyRuleId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getDpdAtWriteOff() { return dpdAtWriteOff; }
    public WriteOffTriggerType getTriggerType() { return triggerType; }
    public String getReason() { return reason; }
    public String getApprovalReference() { return approvalReference; }
    public WriteOffStatus getStatus() { return status; }
    public String getReversalReason() { return reversalReason; }
    public LocalDateTime getReversedAt() { return reversedAt; }
    public UUID getReversedBy() { return reversedBy; }
    public LocalDate getWriteOffDate() { return writeOffDate; }
    public UUID getInitiatedBy() { return initiatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
