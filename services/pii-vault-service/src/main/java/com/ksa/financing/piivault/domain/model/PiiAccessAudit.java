package com.ksa.financing.piivault.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PiiAccessAudit {
    private UUID auditId;
    private UUID piiIndividualId;
    private UUID piiBusinessId;
    private UUID globalUid;
    private UUID accessorId;
    private String accessorRole;
    private String accessorIp;
    private List<String> accessedFields;
    private String accessOperation;
    private AccessPurpose accessPurpose;
    private String accessJustification;
    private boolean accessGranted;
    private String denialReason;
    private Instant accessedAt;

    public UUID getAuditId() { return auditId; }
    public void setAuditId(UUID auditId) { this.auditId = auditId; }
    public UUID getPiiIndividualId() { return piiIndividualId; }
    public void setPiiIndividualId(UUID piiIndividualId) { this.piiIndividualId = piiIndividualId; }
    public UUID getPiiBusinessId() { return piiBusinessId; }
    public void setPiiBusinessId(UUID piiBusinessId) { this.piiBusinessId = piiBusinessId; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public UUID getAccessorId() { return accessorId; }
    public void setAccessorId(UUID accessorId) { this.accessorId = accessorId; }
    public String getAccessorRole() { return accessorRole; }
    public void setAccessorRole(String accessorRole) { this.accessorRole = accessorRole; }
    public String getAccessorIp() { return accessorIp; }
    public void setAccessorIp(String accessorIp) { this.accessorIp = accessorIp; }
    public List<String> getAccessedFields() { return accessedFields; }
    public void setAccessedFields(List<String> accessedFields) { this.accessedFields = accessedFields; }
    public String getAccessOperation() { return accessOperation; }
    public void setAccessOperation(String accessOperation) { this.accessOperation = accessOperation; }
    public AccessPurpose getAccessPurpose() { return accessPurpose; }
    public void setAccessPurpose(AccessPurpose accessPurpose) { this.accessPurpose = accessPurpose; }
    public String getAccessJustification() { return accessJustification; }
    public void setAccessJustification(String accessJustification) { this.accessJustification = accessJustification; }
    public boolean isAccessGranted() { return accessGranted; }
    public void setAccessGranted(boolean accessGranted) { this.accessGranted = accessGranted; }
    public String getDenialReason() { return denialReason; }
    public void setDenialReason(String denialReason) { this.denialReason = denialReason; }
    public Instant getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Instant accessedAt) { this.accessedAt = accessedAt; }
}
