package com.ksa.financing.middleware.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ProviderApi {
    private UUID id;
    private UUID tenantId;
    private UUID providerId;
    private String code;
    private String name;
    private String description;
    private HttpMethod httpMethod;
    private String endpointPath;
    private ApiStatus status;
    private boolean async;
    private Integer timeoutMs;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private Instant deletedAt;
    private int version;

    public ProviderApi() {}

    public static ProviderApi create(UUID tenantId, UUID providerId, String code, String name,
                                      String description, HttpMethod httpMethod, String endpointPath,
                                      boolean async, Integer timeoutMs, UUID createdBy) {
        var api = new ProviderApi();
        api.tenantId = tenantId;
        api.providerId = providerId;
        api.code = code;
        api.name = name;
        api.description = description;
        api.httpMethod = httpMethod;
        api.endpointPath = endpointPath;
        api.status = ApiStatus.ACTIVE;
        api.async = async;
        api.timeoutMs = timeoutMs;
        api.createdBy = createdBy;
        return api;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = ApiStatus.INACTIVE;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProviderId() { return providerId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public HttpMethod getHttpMethod() { return httpMethod; }
    public String getEndpointPath() { return endpointPath; }
    public ApiStatus getStatus() { return status; }
    public boolean isAsync() { return async; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public int getVersion() { return version; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }
    public void setStatus(ApiStatus status) { this.status = status; }
    public void setAsync(boolean async) { this.async = async; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setVersion(int version) { this.version = version; }
}
