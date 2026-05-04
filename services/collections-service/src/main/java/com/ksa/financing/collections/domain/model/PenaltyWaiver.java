package com.ksa.financing.collections.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit aggregate for a single penalty waiver action.
 * Immutable after creation.
 */
public class PenaltyWaiver {

    private final PenaltyWaiverId id;
    private final UUID tenantId;
    private final UUID loanId;
    private final UUID installmentId;

    private final BigDecimal originalPenalty;
    private final BigDecimal waivedAmount;
    private final BigDecimal remainingPenalty;

    private final WaiverType waiverType;
    private final String reason;
    private final String approvalReference;

    private final UUID waivedBy;
    private final LocalDateTime waivedAt;
    private final LocalDateTime createdAt;

    private PenaltyWaiver(PenaltyWaiverId id, UUID tenantId, UUID loanId, UUID installmentId,
                          BigDecimal originalPenalty, BigDecimal waivedAmount,
                          BigDecimal remainingPenalty, WaiverType waiverType,
                          String reason, String approvalReference,
                          UUID waivedBy, LocalDateTime waivedAt, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.loanId = loanId;
        this.installmentId = installmentId;
        this.originalPenalty = originalPenalty;
        this.waivedAmount = waivedAmount;
        this.remainingPenalty = remainingPenalty;
        this.waiverType = waiverType;
        this.reason = reason;
        this.approvalReference = approvalReference;
        this.waivedBy = waivedBy;
        this.waivedAt = waivedAt;
        this.createdAt = createdAt;
    }

    public static PenaltyWaiver create(UUID tenantId, UUID loanId, UUID installmentId,
                                       BigDecimal originalPenalty, BigDecimal waivedAmount,
                                       String reason, String approvalReference, UUID waivedBy) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId required");
        if (loanId == null) throw new IllegalArgumentException("loanId required");
        if (installmentId == null) throw new IllegalArgumentException("installmentId required");
        if (waivedBy == null) throw new IllegalArgumentException("waivedBy required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason required");
        if (originalPenalty == null || originalPenalty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("originalPenalty must be non-negative");
        }
        if (waivedAmount == null || waivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("waivedAmount must be positive");
        }
        if (waivedAmount.compareTo(originalPenalty) > 0) {
            throw new IllegalArgumentException("waivedAmount cannot exceed originalPenalty");
        }

        BigDecimal remaining = originalPenalty.subtract(waivedAmount);
        WaiverType type = remaining.compareTo(BigDecimal.ZERO) == 0 ? WaiverType.FULL : WaiverType.PARTIAL;
        LocalDateTime now = LocalDateTime.now();

        return new PenaltyWaiver(PenaltyWaiverId.generate(), tenantId, loanId, installmentId,
                originalPenalty, waivedAmount, remaining, type, reason, approvalReference,
                waivedBy, now, now);
    }

    public static PenaltyWaiver hydrate(PenaltyWaiverId id, UUID tenantId, UUID loanId, UUID installmentId,
                                        BigDecimal originalPenalty, BigDecimal waivedAmount,
                                        BigDecimal remainingPenalty, WaiverType waiverType,
                                        String reason, String approvalReference,
                                        UUID waivedBy, LocalDateTime waivedAt, LocalDateTime createdAt) {
        return new PenaltyWaiver(id, tenantId, loanId, installmentId, originalPenalty,
                waivedAmount, remainingPenalty, waiverType, reason, approvalReference,
                waivedBy, waivedAt, createdAt);
    }

    public PenaltyWaiverId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getLoanId() { return loanId; }
    public UUID getInstallmentId() { return installmentId; }
    public BigDecimal getOriginalPenalty() { return originalPenalty; }
    public BigDecimal getWaivedAmount() { return waivedAmount; }
    public BigDecimal getRemainingPenalty() { return remainingPenalty; }
    public WaiverType getWaiverType() { return waiverType; }
    public String getReason() { return reason; }
    public String getApprovalReference() { return approvalReference; }
    public UUID getWaivedBy() { return waivedBy; }
    public LocalDateTime getWaivedAt() { return waivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
