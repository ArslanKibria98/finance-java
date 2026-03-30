package com.ksa.financing.risk.domain.model;

import java.time.Instant;
import java.util.UUID;

public class FraudRule {

    private UUID id;
    private UUID tenantId;
    private String ruleId;
    private String scenarioName;
    private String scenarioNameAr;
    private String category;
    private String detectionLogic;
    private String defaultAction;
    private String blockType;
    private String status;
    private String parameters;
    private int priority;
    private Instant createdAt;
    private Instant updatedAt;

    public FraudRule() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public String getScenarioNameAr() { return scenarioNameAr; }
    public void setScenarioNameAr(String scenarioNameAr) { this.scenarioNameAr = scenarioNameAr; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDetectionLogic() { return detectionLogic; }
    public void setDetectionLogic(String detectionLogic) { this.detectionLogic = detectionLogic; }

    public String getDefaultAction() { return defaultAction; }
    public void setDefaultAction(String defaultAction) { this.defaultAction = defaultAction; }

    public String getBlockType() { return blockType; }
    public void setBlockType(String blockType) { this.blockType = blockType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
