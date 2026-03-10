package com.ksa.financing.identity.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserMetadata {
    private UUID id;
    private UUID tenantId;
    private UUID userIdentityId;
    private String displayName;
    private String displayNameAr;
    private String email;
    private String phone;
    private String preferredLanguage;
    private String avatarUrl;
    private Map<String, Object> deviceTokens;
    private Map<String, Object> preferences;
    private boolean mfaEnabled;
    private List<String> mfaMethods;
    private Instant lastLoginAt;
    private Instant lastActivityAt;
    private int loginCount;
    private String lastIpAddress;
    private List<String> trustedIps;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserIdentityId() { return userIdentityId; }
    public void setUserIdentityId(UUID userIdentityId) { this.userIdentityId = userIdentityId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDisplayNameAr() { return displayNameAr; }
    public void setDisplayNameAr(String displayNameAr) { this.displayNameAr = displayNameAr; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Map<String, Object> getDeviceTokens() { return deviceTokens; }
    public void setDeviceTokens(Map<String, Object> deviceTokens) { this.deviceTokens = deviceTokens; }
    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    public List<String> getMfaMethods() { return mfaMethods; }
    public void setMfaMethods(List<String> mfaMethods) { this.mfaMethods = mfaMethods; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }
    public String getLastIpAddress() { return lastIpAddress; }
    public void setLastIpAddress(String lastIpAddress) { this.lastIpAddress = lastIpAddress; }
    public List<String> getTrustedIps() { return trustedIps; }
    public void setTrustedIps(List<String> trustedIps) { this.trustedIps = trustedIps; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
