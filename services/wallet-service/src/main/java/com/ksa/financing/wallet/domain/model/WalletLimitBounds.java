package com.ksa.financing.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Admin-configured platform bounds (per tenant) that constrain the range a customer may
 * request for their wallet transaction limits, plus the defaults applied to new wallets.
 */
public class WalletLimitBounds {

    private UUID id;
    private UUID tenantId;
    private BigDecimal minDailyLimit;
    private BigDecimal maxDailyLimit;
    private BigDecimal minMonthlyLimit;
    private BigDecimal maxMonthlyLimit;
    private BigDecimal minYearlyLimit;
    private BigDecimal maxYearlyLimit;
    private BigDecimal defaultDailyLimit;
    private BigDecimal defaultMonthlyLimit;
    private BigDecimal defaultYearlyLimit;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID updatedBy;
    private int version;

    /** Whether the given daily limit falls within [min, max]. */
    public boolean isDailyWithinBounds(BigDecimal value) {
        return value != null
                && value.compareTo(minDailyLimit) >= 0
                && value.compareTo(maxDailyLimit) <= 0;
    }

    /** Whether the given monthly limit falls within [min, max]. */
    public boolean isMonthlyWithinBounds(BigDecimal value) {
        return value != null
                && value.compareTo(minMonthlyLimit) >= 0
                && value.compareTo(maxMonthlyLimit) <= 0;
    }

    /** Whether the given yearly limit falls within [min, max]. */
    public boolean isYearlyWithinBounds(BigDecimal value) {
        return value != null
                && value.compareTo(minYearlyLimit) >= 0
                && value.compareTo(maxYearlyLimit) <= 0;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public BigDecimal getMinDailyLimit() { return minDailyLimit; }
    public void setMinDailyLimit(BigDecimal minDailyLimit) { this.minDailyLimit = minDailyLimit; }
    public BigDecimal getMaxDailyLimit() { return maxDailyLimit; }
    public void setMaxDailyLimit(BigDecimal maxDailyLimit) { this.maxDailyLimit = maxDailyLimit; }
    public BigDecimal getMinMonthlyLimit() { return minMonthlyLimit; }
    public void setMinMonthlyLimit(BigDecimal minMonthlyLimit) { this.minMonthlyLimit = minMonthlyLimit; }
    public BigDecimal getMaxMonthlyLimit() { return maxMonthlyLimit; }
    public void setMaxMonthlyLimit(BigDecimal maxMonthlyLimit) { this.maxMonthlyLimit = maxMonthlyLimit; }
    public BigDecimal getMinYearlyLimit() { return minYearlyLimit; }
    public void setMinYearlyLimit(BigDecimal minYearlyLimit) { this.minYearlyLimit = minYearlyLimit; }
    public BigDecimal getMaxYearlyLimit() { return maxYearlyLimit; }
    public void setMaxYearlyLimit(BigDecimal maxYearlyLimit) { this.maxYearlyLimit = maxYearlyLimit; }
    public BigDecimal getDefaultDailyLimit() { return defaultDailyLimit; }
    public void setDefaultDailyLimit(BigDecimal defaultDailyLimit) { this.defaultDailyLimit = defaultDailyLimit; }
    public BigDecimal getDefaultMonthlyLimit() { return defaultMonthlyLimit; }
    public void setDefaultMonthlyLimit(BigDecimal defaultMonthlyLimit) { this.defaultMonthlyLimit = defaultMonthlyLimit; }
    public BigDecimal getDefaultYearlyLimit() { return defaultYearlyLimit; }
    public void setDefaultYearlyLimit(BigDecimal defaultYearlyLimit) { this.defaultYearlyLimit = defaultYearlyLimit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
