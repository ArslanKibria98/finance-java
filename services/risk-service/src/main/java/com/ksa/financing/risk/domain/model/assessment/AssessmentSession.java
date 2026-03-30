package com.ksa.financing.risk.domain.model.assessment;

import com.ksa.financing.risk.domain.model.parameter.RiskType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AssessmentSession {
    private UUID id;
    private UUID tenantId;
    private RiskType riskType;
    private String entityReference;
    private UUID parentSessionId;
    private AssessmentSessionStatus status;
    private BigDecimal totalScore;
    private String riskLevel;
    private boolean pepFlag;
    private boolean eddFlag;
    private boolean kycFlag;
    private boolean dominantOverride;
    private String thirdPartyAmlResult;
    private String thirdPartySanctionsResult;
    private String thirdPartyLocalTestResult;
    private String thirdPartyBlocklistResult;
    private String thirdPartyFailureStatus;
    private int parameterVersionSnapshot;
    private int lovVersionSnapshot;
    private UUID initiatedBy;
    private UUID submittedBy;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public RiskType getRiskType() { return riskType; }
    public void setRiskType(RiskType riskType) { this.riskType = riskType; }
    public String getEntityReference() { return entityReference; }
    public void setEntityReference(String entityReference) { this.entityReference = entityReference; }
    public UUID getParentSessionId() { return parentSessionId; }
    public void setParentSessionId(UUID parentSessionId) { this.parentSessionId = parentSessionId; }
    public AssessmentSessionStatus getStatus() { return status; }
    public void setStatus(AssessmentSessionStatus status) { this.status = status; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public boolean isPepFlag() { return pepFlag; }
    public void setPepFlag(boolean pepFlag) { this.pepFlag = pepFlag; }
    public boolean isEddFlag() { return eddFlag; }
    public void setEddFlag(boolean eddFlag) { this.eddFlag = eddFlag; }
    public boolean isKycFlag() { return kycFlag; }
    public void setKycFlag(boolean kycFlag) { this.kycFlag = kycFlag; }
    public boolean isDominantOverride() { return dominantOverride; }
    public void setDominantOverride(boolean dominantOverride) { this.dominantOverride = dominantOverride; }
    public String getThirdPartyAmlResult() { return thirdPartyAmlResult; }
    public void setThirdPartyAmlResult(String thirdPartyAmlResult) { this.thirdPartyAmlResult = thirdPartyAmlResult; }
    public String getThirdPartySanctionsResult() { return thirdPartySanctionsResult; }
    public void setThirdPartySanctionsResult(String thirdPartySanctionsResult) { this.thirdPartySanctionsResult = thirdPartySanctionsResult; }
    public String getThirdPartyLocalTestResult() { return thirdPartyLocalTestResult; }
    public void setThirdPartyLocalTestResult(String thirdPartyLocalTestResult) { this.thirdPartyLocalTestResult = thirdPartyLocalTestResult; }
    public String getThirdPartyBlocklistResult() { return thirdPartyBlocklistResult; }
    public void setThirdPartyBlocklistResult(String thirdPartyBlocklistResult) { this.thirdPartyBlocklistResult = thirdPartyBlocklistResult; }
    public String getThirdPartyFailureStatus() { return thirdPartyFailureStatus; }
    public void setThirdPartyFailureStatus(String thirdPartyFailureStatus) { this.thirdPartyFailureStatus = thirdPartyFailureStatus; }
    public int getParameterVersionSnapshot() { return parameterVersionSnapshot; }
    public void setParameterVersionSnapshot(int parameterVersionSnapshot) { this.parameterVersionSnapshot = parameterVersionSnapshot; }
    public int getLovVersionSnapshot() { return lovVersionSnapshot; }
    public void setLovVersionSnapshot(int lovVersionSnapshot) { this.lovVersionSnapshot = lovVersionSnapshot; }
    public UUID getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(UUID initiatedBy) { this.initiatedBy = initiatedBy; }
    public UUID getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(UUID submittedBy) { this.submittedBy = submittedBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
