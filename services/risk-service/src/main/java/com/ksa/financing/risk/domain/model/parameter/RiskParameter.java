package com.ksa.financing.risk.domain.model.parameter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RiskParameter {
    private UUID id;
    private UUID tenantId;
    private RiskType riskType;
    private String flow;
    private String category;
    private String subCategory;
    private String questionEn;
    private String questionAr;
    private ParameterInputType inputType;
    private UUID lovSetId;
    private UUID parentParameterId;
    private String parentTriggerValue;
    private BigDecimal categoryWeight;
    private String operator;
    private String expectedValue;
    private ParameterFlagType flagType;
    private FilledByType filledBy;
    private boolean active;
    private int displayOrder;
    private String language;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public RiskType getRiskType() { return riskType; }
    public void setRiskType(RiskType riskType) { this.riskType = riskType; }
    public String getFlow() { return flow; }
    public void setFlow(String flow) { this.flow = flow; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getQuestionEn() { return questionEn; }
    public void setQuestionEn(String questionEn) { this.questionEn = questionEn; }
    public String getQuestionAr() { return questionAr; }
    public void setQuestionAr(String questionAr) { this.questionAr = questionAr; }
    public ParameterInputType getInputType() { return inputType; }
    public void setInputType(ParameterInputType inputType) { this.inputType = inputType; }
    public UUID getLovSetId() { return lovSetId; }
    public void setLovSetId(UUID lovSetId) { this.lovSetId = lovSetId; }
    public UUID getParentParameterId() { return parentParameterId; }
    public void setParentParameterId(UUID parentParameterId) { this.parentParameterId = parentParameterId; }
    public String getParentTriggerValue() { return parentTriggerValue; }
    public void setParentTriggerValue(String parentTriggerValue) { this.parentTriggerValue = parentTriggerValue; }
    public BigDecimal getCategoryWeight() { return categoryWeight; }
    public void setCategoryWeight(BigDecimal categoryWeight) { this.categoryWeight = categoryWeight; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    public ParameterFlagType getFlagType() { return flagType; }
    public void setFlagType(ParameterFlagType flagType) { this.flagType = flagType; }
    public FilledByType getFilledBy() { return filledBy; }
    public void setFilledBy(FilledByType filledBy) { this.filledBy = filledBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
