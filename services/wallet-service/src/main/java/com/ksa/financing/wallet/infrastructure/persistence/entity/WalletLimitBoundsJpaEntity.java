package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_limit_bounds")
public class WalletLimitBoundsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "min_single_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal minSingleLimit;

    @Column(name = "max_single_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal maxSingleLimit;

    @Column(name = "default_single_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal defaultSingleLimit;

    @Column(name = "min_daily_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal minDailyLimit;

    @Column(name = "max_daily_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal maxDailyLimit;

    @Column(name = "min_weekly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal minWeeklyLimit;

    @Column(name = "max_weekly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal maxWeeklyLimit;

    @Column(name = "min_monthly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal minMonthlyLimit;

    @Column(name = "max_monthly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal maxMonthlyLimit;

    @Column(name = "min_yearly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal minYearlyLimit;

    @Column(name = "max_yearly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal maxYearlyLimit;

    @Column(name = "default_daily_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal defaultDailyLimit;

    @Column(name = "default_weekly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal defaultWeeklyLimit;

    @Column(name = "default_monthly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal defaultMonthlyLimit;

    @Column(name = "default_yearly_limit", nullable = false, precision = 20, scale = 6)
    private BigDecimal defaultYearlyLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public BigDecimal getMinSingleLimit() { return minSingleLimit; }
    public void setMinSingleLimit(BigDecimal minSingleLimit) { this.minSingleLimit = minSingleLimit; }
    public BigDecimal getMaxSingleLimit() { return maxSingleLimit; }
    public void setMaxSingleLimit(BigDecimal maxSingleLimit) { this.maxSingleLimit = maxSingleLimit; }
    public BigDecimal getDefaultSingleLimit() { return defaultSingleLimit; }
    public void setDefaultSingleLimit(BigDecimal defaultSingleLimit) { this.defaultSingleLimit = defaultSingleLimit; }
    public BigDecimal getMinDailyLimit() { return minDailyLimit; }
    public void setMinDailyLimit(BigDecimal minDailyLimit) { this.minDailyLimit = minDailyLimit; }
    public BigDecimal getMaxDailyLimit() { return maxDailyLimit; }
    public void setMaxDailyLimit(BigDecimal maxDailyLimit) { this.maxDailyLimit = maxDailyLimit; }
    public BigDecimal getMinWeeklyLimit() { return minWeeklyLimit; }
    public void setMinWeeklyLimit(BigDecimal minWeeklyLimit) { this.minWeeklyLimit = minWeeklyLimit; }
    public BigDecimal getMaxWeeklyLimit() { return maxWeeklyLimit; }
    public void setMaxWeeklyLimit(BigDecimal maxWeeklyLimit) { this.maxWeeklyLimit = maxWeeklyLimit; }
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
    public BigDecimal getDefaultWeeklyLimit() { return defaultWeeklyLimit; }
    public void setDefaultWeeklyLimit(BigDecimal defaultWeeklyLimit) { this.defaultWeeklyLimit = defaultWeeklyLimit; }
    public BigDecimal getDefaultMonthlyLimit() { return defaultMonthlyLimit; }
    public void setDefaultMonthlyLimit(BigDecimal defaultMonthlyLimit) { this.defaultMonthlyLimit = defaultMonthlyLimit; }
    public BigDecimal getDefaultYearlyLimit() { return defaultYearlyLimit; }
    public void setDefaultYearlyLimit(BigDecimal defaultYearlyLimit) { this.defaultYearlyLimit = defaultYearlyLimit; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
