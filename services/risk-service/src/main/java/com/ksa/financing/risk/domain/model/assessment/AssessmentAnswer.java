package com.ksa.financing.risk.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AssessmentAnswer {
    private UUID id;
    private UUID sessionId;
    private UUID tenantId;
    private UUID parameterId;
    private int answerVersion;
    private AnswerVersionStatus versionStatus;
    private String answerValue;
    private String answerType;
    private String languageCode;
    private BigDecimal weightContribution;
    private BigDecimal riskScoreAtSubmission;
    private String riskLevelAtSubmission;
    private AnswerChangeReason changeReason;
    private BigDecimal scoreDelta;
    private boolean levelChanged;
    private String previousRiskLevel;
    private String newRiskLevel;
    private UUID submittedBy;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getParameterId() { return parameterId; }
    public void setParameterId(UUID parameterId) { this.parameterId = parameterId; }
    public int getAnswerVersion() { return answerVersion; }
    public void setAnswerVersion(int answerVersion) { this.answerVersion = answerVersion; }
    public AnswerVersionStatus getVersionStatus() { return versionStatus; }
    public void setVersionStatus(AnswerVersionStatus versionStatus) { this.versionStatus = versionStatus; }
    public String getAnswerValue() { return answerValue; }
    public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }
    public String getAnswerType() { return answerType; }
    public void setAnswerType(String answerType) { this.answerType = answerType; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public BigDecimal getWeightContribution() { return weightContribution; }
    public void setWeightContribution(BigDecimal weightContribution) { this.weightContribution = weightContribution; }
    public BigDecimal getRiskScoreAtSubmission() { return riskScoreAtSubmission; }
    public void setRiskScoreAtSubmission(BigDecimal riskScoreAtSubmission) { this.riskScoreAtSubmission = riskScoreAtSubmission; }
    public String getRiskLevelAtSubmission() { return riskLevelAtSubmission; }
    public void setRiskLevelAtSubmission(String riskLevelAtSubmission) { this.riskLevelAtSubmission = riskLevelAtSubmission; }
    public AnswerChangeReason getChangeReason() { return changeReason; }
    public void setChangeReason(AnswerChangeReason changeReason) { this.changeReason = changeReason; }
    public BigDecimal getScoreDelta() { return scoreDelta; }
    public void setScoreDelta(BigDecimal scoreDelta) { this.scoreDelta = scoreDelta; }
    public boolean isLevelChanged() { return levelChanged; }
    public void setLevelChanged(boolean levelChanged) { this.levelChanged = levelChanged; }
    public String getPreviousRiskLevel() { return previousRiskLevel; }
    public void setPreviousRiskLevel(String previousRiskLevel) { this.previousRiskLevel = previousRiskLevel; }
    public String getNewRiskLevel() { return newRiskLevel; }
    public void setNewRiskLevel(String newRiskLevel) { this.newRiskLevel = newRiskLevel; }
    public UUID getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(UUID submittedBy) { this.submittedBy = submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
