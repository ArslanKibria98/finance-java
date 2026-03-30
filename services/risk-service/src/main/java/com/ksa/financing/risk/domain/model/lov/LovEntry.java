package com.ksa.financing.risk.domain.model.lov;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LovEntry {
    private UUID id;
    private UUID lovSetId;
    private UUID tenantId;
    private String factorCode;
    private String labelEn;
    private String labelAr;
    private BigDecimal factorWeight;
    private String riskStatus;
    private int lovVersion;
    private boolean active;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLovSetId() { return lovSetId; }
    public void setLovSetId(UUID lovSetId) { this.lovSetId = lovSetId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getFactorCode() { return factorCode; }
    public void setFactorCode(String factorCode) { this.factorCode = factorCode; }
    public String getLabelEn() { return labelEn; }
    public void setLabelEn(String labelEn) { this.labelEn = labelEn; }
    public String getLabelAr() { return labelAr; }
    public void setLabelAr(String labelAr) { this.labelAr = labelAr; }
    public BigDecimal getFactorWeight() { return factorWeight; }
    public void setFactorWeight(BigDecimal factorWeight) { this.factorWeight = factorWeight; }
    public String getRiskStatus() { return riskStatus; }
    public void setRiskStatus(String riskStatus) { this.riskStatus = riskStatus; }
    public int getLovVersion() { return lovVersion; }
    public void setLovVersion(int lovVersion) { this.lovVersion = lovVersion; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
