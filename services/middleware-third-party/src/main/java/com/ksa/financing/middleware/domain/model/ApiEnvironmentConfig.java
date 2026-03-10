package com.ksa.financing.middleware.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ApiEnvironmentConfig {
    private UUID id;
    private UUID tenantId;
    private UUID apiId;
    private EnvironmentType environment;
    private String baseUrl;
    private String endpointPath;
    private String credentials;   // JSON string
    private String headers;       // JSON string
    private String queryParams;   // JSON string
    private AuthType authType;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private Instant deletedAt;
    private int version;

    public ApiEnvironmentConfig() {}

    public static ApiEnvironmentConfig create(UUID tenantId, UUID apiId, EnvironmentType environment,
                                               String baseUrl, String endpointPath, String credentials,
                                               String headers, String queryParams, AuthType authType,
                                               UUID createdBy) {
        var config = new ApiEnvironmentConfig();
        config.tenantId = tenantId;
        config.apiId = apiId;
        config.environment = environment;
        config.baseUrl = baseUrl;
        config.endpointPath = endpointPath;
        config.credentials = credentials;
        config.headers = headers;
        config.queryParams = queryParams;
        config.authType = authType;
        config.active = true;
        config.createdBy = createdBy;
        return config;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.active = false;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getApiId() { return apiId; }
    public EnvironmentType getEnvironment() { return environment; }
    public String getBaseUrl() { return baseUrl; }
    public String getEndpointPath() { return endpointPath; }
    public String getCredentials() { return credentials; }
    public String getHeaders() { return headers; }
    public String getQueryParams() { return queryParams; }
    public AuthType getAuthType() { return authType; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public int getVersion() { return version; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setApiId(UUID apiId) { this.apiId = apiId; }
    public void setEnvironment(EnvironmentType environment) { this.environment = environment; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }
    public void setCredentials(String credentials) { this.credentials = credentials; }
    public void setHeaders(String headers) { this.headers = headers; }
    public void setQueryParams(String queryParams) { this.queryParams = queryParams; }
    public void setAuthType(AuthType authType) { this.authType = authType; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setVersion(int version) { this.version = version; }
}
