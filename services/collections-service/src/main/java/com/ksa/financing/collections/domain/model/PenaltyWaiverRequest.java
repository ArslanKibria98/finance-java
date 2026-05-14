package com.ksa.financing.collections.domain.model;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class PenaltyWaiverRequest {
    private final UUID id;
    private final UUID tenantId;
    private final UUID loanId;
    private final UUID applicationId;
    private final String invoiceId;
    private final UUID installmentId;
    private final BigDecimal requestedAmount;
    private final String reason;
    private WaiverRequestStatus status;

    private final UUID requestedBy;
    private final LocalDateTime requestedAt;

    private UUID processedBy;
    private LocalDateTime processedAt;
    private String rejectionReason;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum WaiverRequestStatus {
        PENDING, APPROVED, REJECTED
    }

    private PenaltyWaiverRequest(UUID id, UUID tenantId, UUID loanId, UUID applicationId, String invoiceId, UUID installmentId,
                                BigDecimal requestedAmount, String reason, WaiverRequestStatus status,
                                UUID requestedBy, LocalDateTime requestedAt,
                                LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.loanId = loanId;
        this.applicationId = applicationId;
        this.invoiceId = invoiceId;
        this.installmentId = installmentId;
        this.requestedAmount = requestedAmount;
        this.reason = reason;
        this.status = status;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static PenaltyWaiverRequest create(UUID tenantId, UUID loanId, UUID applicationId, String invoiceId, UUID installmentId,
                                             BigDecimal requestedAmount, String reason, UUID requestedBy) {
        return new PenaltyWaiverRequest(UUID.randomUUID(), tenantId, loanId, applicationId, invoiceId, installmentId,
                requestedAmount, reason, WaiverRequestStatus.PENDING,
                requestedBy, LocalDateTime.now(), LocalDateTime.now());
    }

    public void approve(UUID processedBy) {
        if (this.status != WaiverRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved");
        }
        this.status = WaiverRequestStatus.APPROVED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(UUID processedBy, String reason) {
        if (this.status != WaiverRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected");
        }
        this.status = WaiverRequestStatus.REJECTED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public static PenaltyWaiverRequest reconstitute(UUID id, UUID tenantId, UUID loanId, UUID applicationId, String invoiceId, UUID installmentId,
                                                   BigDecimal requestedAmount, String reason, WaiverRequestStatus status,
                                                   UUID requestedBy, LocalDateTime requestedAt,
                                                   UUID processedBy, LocalDateTime processedAt, String rejectionReason,
                                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        var request = new PenaltyWaiverRequest(id, tenantId, loanId, applicationId, invoiceId, installmentId, requestedAmount, reason, status,
                requestedBy, requestedAt, createdAt);
        request.processedBy = processedBy;
        request.processedAt = processedAt;
        request.rejectionReason = rejectionReason;
        request.updatedAt = updatedAt;
        return request;
    }
}
