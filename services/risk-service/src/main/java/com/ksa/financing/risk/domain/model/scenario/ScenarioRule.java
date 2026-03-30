package com.ksa.financing.risk.domain.model.scenario;

import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;

import java.time.Instant;
import java.util.UUID;

public class ScenarioRule {
    private UUID id;
    private UUID tenantId;
    private String scenarioName;
    private String scenarioNameAr;
    private String triggerRiskStatus;
    private Boolean triggerPepFlag;
    private ThirdPartyCheckType triggerThirdPartyCheckType;
    private String triggerThirdPartyResult;
    private AccountStatus resultingAccountStatus;
    private ComplianceStatus resultingComplianceStatus;
    private boolean requiresManualReview;
    private String notifyRole;
    private int priority;
    private int slaDurationHours;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getScenarioNameAr() { return scenarioNameAr; }
    public void setScenarioNameAr(String scenarioNameAr) { this.scenarioNameAr = scenarioNameAr; }
    public String getTriggerRiskStatus() { return triggerRiskStatus; }
    public void setTriggerRiskStatus(String triggerRiskStatus) { this.triggerRiskStatus = triggerRiskStatus; }
    public Boolean getTriggerPepFlag() { return triggerPepFlag; }
    public void setTriggerPepFlag(Boolean triggerPepFlag) { this.triggerPepFlag = triggerPepFlag; }
    public ThirdPartyCheckType getTriggerThirdPartyCheckType() { return triggerThirdPartyCheckType; }
    public void setTriggerThirdPartyCheckType(ThirdPartyCheckType triggerThirdPartyCheckType) { this.triggerThirdPartyCheckType = triggerThirdPartyCheckType; }
    public String getTriggerThirdPartyResult() { return triggerThirdPartyResult; }
    public void setTriggerThirdPartyResult(String triggerThirdPartyResult) { this.triggerThirdPartyResult = triggerThirdPartyResult; }
    public AccountStatus getResultingAccountStatus() { return resultingAccountStatus; }
    public void setResultingAccountStatus(AccountStatus resultingAccountStatus) { this.resultingAccountStatus = resultingAccountStatus; }
    public ComplianceStatus getResultingComplianceStatus() { return resultingComplianceStatus; }
    public void setResultingComplianceStatus(ComplianceStatus resultingComplianceStatus) { this.resultingComplianceStatus = resultingComplianceStatus; }
    public boolean isRequiresManualReview() { return requiresManualReview; }
    public void setRequiresManualReview(boolean requiresManualReview) { this.requiresManualReview = requiresManualReview; }
    public String getNotifyRole() { return notifyRole; }
    public void setNotifyRole(String notifyRole) { this.notifyRole = notifyRole; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getSlaDurationHours() { return slaDurationHours; }
    public void setSlaDurationHours(int slaDurationHours) { this.slaDurationHours = slaDurationHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
