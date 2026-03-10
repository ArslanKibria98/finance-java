package com.ksa.financing.identity.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class UserSession {
    private UUID id;
    private UUID tenantId;
    private UUID userIdentityId;
    private String sessionId;
    private String keycloakSessionId;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String userAgent;
    private String ipAddress;
    private Map<String, Object> geoLocation;
    private SessionStatus status;
    private Instant createdAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private Instant revokedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserIdentityId() { return userIdentityId; }
    public void setUserIdentityId(UUID userIdentityId) { this.userIdentityId = userIdentityId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getKeycloakSessionId() { return keycloakSessionId; }
    public void setKeycloakSessionId(String keycloakSessionId) { this.keycloakSessionId = keycloakSessionId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Map<String, Object> getGeoLocation() { return geoLocation; }
    public void setGeoLocation(Map<String, Object> geoLocation) { this.geoLocation = geoLocation; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
