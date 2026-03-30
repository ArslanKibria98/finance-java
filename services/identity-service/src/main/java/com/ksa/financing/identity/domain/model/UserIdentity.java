package com.ksa.financing.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UserIdentity {
    private UUID id;
    private UUID tenantId;
    private UUID keycloakUserId;
    private String keycloakRealm;
    private String keycloakUsername;
    private UUID internalUserId;
    private UUID internalCustomerId;
    private UUID internalPartnerId;
    private UUID globalUid;
    private UserType userType;
    private String mobileNumber;
    private UserStatus status;
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(UUID keycloakUserId) { this.keycloakUserId = keycloakUserId; }
    public String getKeycloakRealm() { return keycloakRealm; }
    public void setKeycloakRealm(String keycloakRealm) { this.keycloakRealm = keycloakRealm; }
    public String getKeycloakUsername() { return keycloakUsername; }
    public void setKeycloakUsername(String keycloakUsername) { this.keycloakUsername = keycloakUsername; }
    public UUID getInternalUserId() { return internalUserId; }
    public void setInternalUserId(UUID internalUserId) { this.internalUserId = internalUserId; }
    public UUID getInternalCustomerId() { return internalCustomerId; }
    public void setInternalCustomerId(UUID internalCustomerId) { this.internalCustomerId = internalCustomerId; }
    public UUID getInternalPartnerId() { return internalPartnerId; }
    public void setInternalPartnerId(UUID internalPartnerId) { this.internalPartnerId = internalPartnerId; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
