package com.ksa.financing.globalprofile.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PiiAccessToken {
    private UUID tokenId;
    private String accessToken;
    private String tokenHash;
    private UUID globalUid;
    private UUID regionalProfileId;
    private String piiVaultRegion;
    private List<String> allowedFields;
    private UUID requesterId;
    private String requesterRole;
    private String requesterIp;
    private String accessPurpose;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
    private String revokedReason;
    private int usedCount;
    private Instant lastUsedAt;

    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public UUID getRegionalProfileId() { return regionalProfileId; }
    public void setRegionalProfileId(UUID regionalProfileId) { this.regionalProfileId = regionalProfileId; }
    public String getPiiVaultRegion() { return piiVaultRegion; }
    public void setPiiVaultRegion(String piiVaultRegion) { this.piiVaultRegion = piiVaultRegion; }
    public List<String> getAllowedFields() { return allowedFields; }
    public void setAllowedFields(List<String> allowedFields) { this.allowedFields = allowedFields; }
    public UUID getRequesterId() { return requesterId; }
    public void setRequesterId(UUID requesterId) { this.requesterId = requesterId; }
    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }
    public String getRequesterIp() { return requesterIp; }
    public void setRequesterIp(String requesterIp) { this.requesterIp = requesterIp; }
    public String getAccessPurpose() { return accessPurpose; }
    public void setAccessPurpose(String accessPurpose) { this.accessPurpose = accessPurpose; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String relatedEntityType) { this.relatedEntityType = relatedEntityType; }
    public UUID getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(UUID relatedEntityId) { this.relatedEntityId = relatedEntityId; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
