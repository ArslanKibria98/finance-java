package com.ksa.financing.risk.domain.model.tenant;

import java.time.Instant;
import java.util.UUID;

public class TenantConfig {
    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private String tenantNameAr;
    private TenantStatus status;
    private boolean customerRiskEnabled;
    private boolean businessRiskEnabled;
    private boolean loanRiskEnabled;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getTenantNameAr() { return tenantNameAr; }
    public void setTenantNameAr(String tenantNameAr) { this.tenantNameAr = tenantNameAr; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public boolean isCustomerRiskEnabled() { return customerRiskEnabled; }
    public void setCustomerRiskEnabled(boolean customerRiskEnabled) { this.customerRiskEnabled = customerRiskEnabled; }
    public boolean isBusinessRiskEnabled() { return businessRiskEnabled; }
    public void setBusinessRiskEnabled(boolean businessRiskEnabled) { this.businessRiskEnabled = businessRiskEnabled; }
    public boolean isLoanRiskEnabled() { return loanRiskEnabled; }
    public void setLoanRiskEnabled(boolean loanRiskEnabled) { this.loanRiskEnabled = loanRiskEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
