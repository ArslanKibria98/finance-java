package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A customer's request to change the daily / monthly transaction (spend) limit on their wallet.
 * Pure domain model — invariants enforced via approve()/reject()/cancel() state transitions.
 */
public class WalletLimitChangeRequest {

    private UUID id;
    private UUID tenantId;
    private UUID walletId;
    private UUID customerId;
    private BigDecimal requestedDailyLimit;
    private BigDecimal requestedMonthlyLimit;
    private BigDecimal requestedYearlyLimit;
    private BigDecimal currentDailyLimit;
    private BigDecimal currentMonthlyLimit;
    private BigDecimal currentYearlyLimit;
    private String reason;
    private LimitRequestStatus status;
    private UUID requestedBy;
    private Instant requestedAt;
    private UUID decisionBy;
    private Instant decisionAt;
    private String decisionNotes;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    /** Approve the request. Only PENDING requests may be approved. */
    public void approve(UUID decisionBy, String notes) {
        if (this.status != LimitRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot approve request in status: " + this.status);
        }
        this.status = LimitRequestStatus.APPROVED;
        this.decisionBy = decisionBy;
        this.decisionAt = Instant.now();
        this.decisionNotes = notes;
    }

    /** Reject the request. Only PENDING requests may be rejected. */
    public void reject(UUID decisionBy, String reason, String notes) {
        if (this.status != LimitRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot reject request in status: " + this.status);
        }
        this.status = LimitRequestStatus.REJECTED;
        this.decisionBy = decisionBy;
        this.decisionAt = Instant.now();
        this.rejectionReason = reason;
        this.decisionNotes = notes;
    }

    /** Cancel a still-pending request (requester action). */
    public void cancel() {
        if (this.status != LimitRequestStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel request in status: " + this.status);
        }
        this.status = LimitRequestStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public BigDecimal getRequestedDailyLimit() { return requestedDailyLimit; }
    public void setRequestedDailyLimit(BigDecimal requestedDailyLimit) { this.requestedDailyLimit = requestedDailyLimit; }
    public BigDecimal getRequestedMonthlyLimit() { return requestedMonthlyLimit; }
    public void setRequestedMonthlyLimit(BigDecimal requestedMonthlyLimit) { this.requestedMonthlyLimit = requestedMonthlyLimit; }
    public BigDecimal getRequestedYearlyLimit() { return requestedYearlyLimit; }
    public void setRequestedYearlyLimit(BigDecimal requestedYearlyLimit) { this.requestedYearlyLimit = requestedYearlyLimit; }
    public BigDecimal getCurrentDailyLimit() { return currentDailyLimit; }
    public void setCurrentDailyLimit(BigDecimal currentDailyLimit) { this.currentDailyLimit = currentDailyLimit; }
    public BigDecimal getCurrentMonthlyLimit() { return currentMonthlyLimit; }
    public void setCurrentMonthlyLimit(BigDecimal currentMonthlyLimit) { this.currentMonthlyLimit = currentMonthlyLimit; }
    public BigDecimal getCurrentYearlyLimit() { return currentYearlyLimit; }
    public void setCurrentYearlyLimit(BigDecimal currentYearlyLimit) { this.currentYearlyLimit = currentYearlyLimit; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LimitRequestStatus getStatus() { return status; }
    public void setStatus(LimitRequestStatus status) { this.status = status; }
    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public UUID getDecisionBy() { return decisionBy; }
    public void setDecisionBy(UUID decisionBy) { this.decisionBy = decisionBy; }
    public Instant getDecisionAt() { return decisionAt; }
    public void setDecisionAt(Instant decisionAt) { this.decisionAt = decisionAt; }
    public String getDecisionNotes() { return decisionNotes; }
    public void setDecisionNotes(String decisionNotes) { this.decisionNotes = decisionNotes; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
