package com.ksa.financing.risk.domain.model.review;

import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;

import java.time.Instant;
import java.util.UUID;

public class ReviewTask {
    private UUID id;
    private UUID tenantId;
    private UUID sessionId;
    private String entityReference;
    private ReviewTaskStatus status;
    private UUID makerId;
    private ReviewRecommendation makerRecommendation;
    private String makerComment;
    private AccountStatus makerAccountStatus;
    private ComplianceStatus makerComplianceStatus;
    private Instant makerActionAt;
    private UUID approverId;
    private ReviewAction approverAction;
    private String approverComment;
    private AccountStatus approverAccountStatus;
    private ComplianceStatus approverComplianceStatus;
    private Instant approverActionAt;
    private Instant slaDeadline;
    private boolean slaBreached;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public String getEntityReference() { return entityReference; }
    public void setEntityReference(String entityReference) { this.entityReference = entityReference; }
    public ReviewTaskStatus getStatus() { return status; }
    public void setStatus(ReviewTaskStatus status) { this.status = status; }
    public UUID getMakerId() { return makerId; }
    public void setMakerId(UUID makerId) { this.makerId = makerId; }
    public ReviewRecommendation getMakerRecommendation() { return makerRecommendation; }
    public void setMakerRecommendation(ReviewRecommendation makerRecommendation) { this.makerRecommendation = makerRecommendation; }
    public String getMakerComment() { return makerComment; }
    public void setMakerComment(String makerComment) { this.makerComment = makerComment; }
    public AccountStatus getMakerAccountStatus() { return makerAccountStatus; }
    public void setMakerAccountStatus(AccountStatus makerAccountStatus) { this.makerAccountStatus = makerAccountStatus; }
    public ComplianceStatus getMakerComplianceStatus() { return makerComplianceStatus; }
    public void setMakerComplianceStatus(ComplianceStatus makerComplianceStatus) { this.makerComplianceStatus = makerComplianceStatus; }
    public Instant getMakerActionAt() { return makerActionAt; }
    public void setMakerActionAt(Instant makerActionAt) { this.makerActionAt = makerActionAt; }
    public UUID getApproverId() { return approverId; }
    public void setApproverId(UUID approverId) { this.approverId = approverId; }
    public ReviewAction getApproverAction() { return approverAction; }
    public void setApproverAction(ReviewAction approverAction) { this.approverAction = approverAction; }
    public String getApproverComment() { return approverComment; }
    public void setApproverComment(String approverComment) { this.approverComment = approverComment; }
    public AccountStatus getApproverAccountStatus() { return approverAccountStatus; }
    public void setApproverAccountStatus(AccountStatus approverAccountStatus) { this.approverAccountStatus = approverAccountStatus; }
    public ComplianceStatus getApproverComplianceStatus() { return approverComplianceStatus; }
    public void setApproverComplianceStatus(ComplianceStatus approverComplianceStatus) { this.approverComplianceStatus = approverComplianceStatus; }
    public Instant getApproverActionAt() { return approverActionAt; }
    public void setApproverActionAt(Instant approverActionAt) { this.approverActionAt = approverActionAt; }
    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }
    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
