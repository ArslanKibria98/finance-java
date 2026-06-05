package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_limit_change_requests")
public class WalletLimitChangeRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "requested_single_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal requestedSingleLimit;

    @Column(name = "requested_daily_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal requestedDailyLimit;

    @Column(name = "requested_monthly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal requestedMonthlyLimit;

    @Column(name = "requested_yearly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal requestedYearlyLimit;

    @Column(name = "current_single_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal currentSingleLimit;

    @Column(name = "current_daily_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal currentDailyLimit;

    @Column(name = "current_monthly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal currentMonthlyLimit;

    @Column(name = "current_yearly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal currentYearlyLimit;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "decision_by")
    private UUID decisionBy;

    @Column(name = "decision_at")
    private OffsetDateTime decisionAt;

    @Column(name = "decision_notes", length = 500)
    private String decisionNotes;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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
        if (requestedAt == null) requestedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public BigDecimal getRequestedSingleLimit() { return requestedSingleLimit; }
    public void setRequestedSingleLimit(BigDecimal requestedSingleLimit) { this.requestedSingleLimit = requestedSingleLimit; }
    public BigDecimal getCurrentSingleLimit() { return currentSingleLimit; }
    public void setCurrentSingleLimit(BigDecimal currentSingleLimit) { this.currentSingleLimit = currentSingleLimit; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(OffsetDateTime requestedAt) { this.requestedAt = requestedAt; }
    public UUID getDecisionBy() { return decisionBy; }
    public void setDecisionBy(UUID decisionBy) { this.decisionBy = decisionBy; }
    public OffsetDateTime getDecisionAt() { return decisionAt; }
    public void setDecisionAt(OffsetDateTime decisionAt) { this.decisionAt = decisionAt; }
    public String getDecisionNotes() { return decisionNotes; }
    public void setDecisionNotes(String decisionNotes) { this.decisionNotes = decisionNotes; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
