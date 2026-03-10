package com.ksa.financing.middleware.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ThirdPartyProvider {
    private UUID id;
    private UUID tenantId;
    private String code;
    private String name;
    private String description;
    private ProviderCategory category;
    private String baseUrlDev;
    private String baseUrlProd;
    private AuthType authType;
    private ProviderStatus status;
    private int timeoutMs;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private Instant deletedAt;
    private int version;

    public ThirdPartyProvider() {}

    public static ThirdPartyProvider create(UUID tenantId, String code, String name, String description,
                                             ProviderCategory category, AuthType authType,
                                             int timeoutMs, int retryCount, UUID createdBy) {
        var provider = new ThirdPartyProvider();
        provider.tenantId = tenantId;
        provider.code = code;
        provider.name = name;
        provider.description = description;
        provider.category = category;
        provider.authType = authType;
        provider.status = ProviderStatus.ACTIVE;
        provider.timeoutMs = timeoutMs;
        provider.retryCount = retryCount;
        provider.createdBy = createdBy;
        return provider;
    }

    public void update(String name, String description, ProviderCategory category,
                       AuthType authType, int timeoutMs, int retryCount) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.authType = authType;
        this.timeoutMs = timeoutMs;
        this.retryCount = retryCount;
    }

    public void deactivate() {
        if (this.status == ProviderStatus.INACTIVE) {
            throw new IllegalStateException("Provider is already inactive");
        }
        this.status = ProviderStatus.INACTIVE;
    }

    public void activate() {
        this.status = ProviderStatus.ACTIVE;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = ProviderStatus.INACTIVE;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProviderCategory getCategory() { return category; }
    public String getBaseUrlDev() { return baseUrlDev; }
    public String getBaseUrlProd() { return baseUrlProd; }
    public AuthType getAuthType() { return authType; }
    public ProviderStatus getStatus() { return status; }
    public int getTimeoutMs() { return timeoutMs; }
    public int getRetryCount() { return retryCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public int getVersion() { return version; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(ProviderCategory category) { this.category = category; }
    public void setBaseUrlDev(String baseUrlDev) { this.baseUrlDev = baseUrlDev; }
    public void setBaseUrlProd(String baseUrlProd) { this.baseUrlProd = baseUrlProd; }
    public void setAuthType(AuthType authType) { this.authType = authType; }
    public void setStatus(ProviderStatus status) { this.status = status; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setVersion(int version) { this.version = version; }
}
