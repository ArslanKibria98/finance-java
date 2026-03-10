package com.ksa.financing.middleware.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ClientProviderAccess {
    private UUID id;
    private UUID tenantId;
    private UUID clientId;
    private UUID providerId;
    private AccessEnvironment environment;
    private boolean active;
    private Instant grantedAt;
    private UUID grantedBy;

    public ClientProviderAccess() {}

    public static ClientProviderAccess grant(UUID tenantId, UUID clientId, UUID providerId,
                                              AccessEnvironment environment, UUID grantedBy) {
        var access = new ClientProviderAccess();
        access.tenantId = tenantId;
        access.clientId = clientId;
        access.providerId = providerId;
        access.environment = environment;
        access.active = true;
        access.grantedBy = grantedBy;
        return access;
    }

    public void revoke() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getClientId() { return clientId; }
    public UUID getProviderId() { return providerId; }
    public AccessEnvironment getEnvironment() { return environment; }
    public boolean isActive() { return active; }
    public Instant getGrantedAt() { return grantedAt; }
    public UUID getGrantedBy() { return grantedBy; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public void setEnvironment(AccessEnvironment environment) { this.environment = environment; }
    public void setActive(boolean active) { this.active = active; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    public void setGrantedBy(UUID grantedBy) { this.grantedBy = grantedBy; }
}
