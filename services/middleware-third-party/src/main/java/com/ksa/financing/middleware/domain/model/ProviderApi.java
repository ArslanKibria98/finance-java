package com.ksa.financing.middleware.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProviderApi {
    private UUID id;
    private UUID tenantId;
    private UUID providerId;
    private String code;
    private String nameEn;
    private String nameAr;
    private String descriptionEn;
    private String descriptionAr;
    private HttpMethod httpMethod;
    private String endpointPath;
    private ApiStatus status;
    private boolean async;
    private Integer timeoutMs;
    private BigDecimal costPerCall;
    private String costCurrency;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private Instant deletedAt;
    private int version;

    public ProviderApi() {}

    public static ProviderApi create(UUID tenantId, UUID providerId, String code, String nameEn,
                                      String nameAr, String descriptionEn, String descriptionAr,
                                      HttpMethod httpMethod, String endpointPath,
                                      boolean async, Integer timeoutMs, UUID createdBy) {
        var api = new ProviderApi();
        api.tenantId = tenantId;
        api.providerId = providerId;
        api.code = code;
        api.nameEn = nameEn;
        api.nameAr = nameAr;
        api.descriptionEn = descriptionEn;
        api.descriptionAr = descriptionAr;
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
    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public String getDescriptionEn() { return descriptionEn; }
    public String getDescriptionAr() { return descriptionAr; }
    public HttpMethod getHttpMethod() { return httpMethod; }
    public String getEndpointPath() { return endpointPath; }
    public ApiStatus getStatus() { return status; }
    public boolean isAsync() { return async; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public BigDecimal getCostPerCall() { return costPerCall; }
    public String getCostCurrency() { return costCurrency; }
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
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public void setDescriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }
    public void setStatus(ApiStatus status) { this.status = status; }
    public void setAsync(boolean async) { this.async = async; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public void setCostPerCall(BigDecimal costPerCall) { this.costPerCall = costPerCall; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setVersion(int version) { this.version = version; }
}
