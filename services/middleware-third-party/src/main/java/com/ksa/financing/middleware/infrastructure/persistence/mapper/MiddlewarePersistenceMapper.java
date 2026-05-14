package com.ksa.financing.middleware.infrastructure.persistence.mapper;

import com.ksa.financing.middleware.domain.model.*;
import com.ksa.financing.middleware.infrastructure.persistence.entity.*;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ThirdPartyProviderJpaEntity.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Component
public class MiddlewarePersistenceMapper {

    // ==================== ThirdPartyProvider ====================

    public ThirdPartyProvider toDomain(ThirdPartyProviderJpaEntity entity) {
        if (entity == null) return null;
        var domain = new ThirdPartyProvider();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setCategory(toProviderCategory(entity.getCategory()));
        domain.setBaseUrlDev(entity.getBaseUrlDev());
        domain.setBaseUrlProd(entity.getBaseUrlProd());
        domain.setAuthType(toAuthType(entity.getAuthType()));
        domain.setStatus(toProviderStatus(entity.getStatus()));
        domain.setTimeoutMs(entity.getTimeoutMs());
        domain.setRetryCount(entity.getRetryCount());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setDeletedAt(toInstant(entity.getDeletedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    public ThirdPartyProviderJpaEntity toEntity(ThirdPartyProvider domain) {
        if (domain == null) return null;
        var entity = new ThirdPartyProviderJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setCategory(toProviderCategoryEnum(domain.getCategory()));
        entity.setBaseUrlDev(domain.getBaseUrlDev());
        entity.setBaseUrlProd(domain.getBaseUrlProd());
        entity.setAuthType(toAuthTypeEnum(domain.getAuthType()));
        entity.setStatus(toProviderStatusEnum(domain.getStatus()));
        entity.setTimeoutMs(domain.getTimeoutMs());
        entity.setRetryCount(domain.getRetryCount());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setDeletedAt(toOffsetDateTime(domain.getDeletedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    // ==================== ProviderApi ====================

    public ProviderApi toDomain(ProviderApiJpaEntity entity) {
        if (entity == null) return null;
        var domain = new ProviderApi();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setProviderId(entity.getProviderId());
        domain.setCode(entity.getCode());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setDescriptionAr(entity.getDescriptionAr());
        domain.setHttpMethod(toHttpMethod(entity.getHttpMethod()));
        domain.setEndpointPath(entity.getEndpointPath());
        domain.setStatus(toApiStatus(entity.getStatus()));
        domain.setAsync(entity.isAsync());
        domain.setTimeoutMs(entity.getTimeoutMs());
        domain.setCostPerCall(entity.getCostPerCall());
        domain.setCostCurrency(entity.getCostCurrency());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setDeletedAt(toInstant(entity.getDeletedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    public ProviderApiJpaEntity toEntity(ProviderApi domain) {
        if (domain == null) return null;
        var entity = new ProviderApiJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setProviderId(domain.getProviderId());
        entity.setCode(domain.getCode());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setDescriptionAr(domain.getDescriptionAr());
        entity.setHttpMethod(toApiHttpMethodEnum(domain.getHttpMethod()));
        entity.setEndpointPath(domain.getEndpointPath());
        entity.setStatus(toApiStatusEnum(domain.getStatus()));
        entity.setAsync(domain.isAsync());
        entity.setTimeoutMs(domain.getTimeoutMs());
        entity.setCostPerCall(domain.getCostPerCall() != null ? domain.getCostPerCall() : java.math.BigDecimal.ZERO);
        entity.setCostCurrency(domain.getCostCurrency() != null ? domain.getCostCurrency() : "SAR");
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setDeletedAt(toOffsetDateTime(domain.getDeletedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    // ==================== ApiEnvironmentConfig ====================

    public ApiEnvironmentConfig toDomain(ApiEnvironmentConfigJpaEntity entity) {
        if (entity == null) return null;
        var domain = new ApiEnvironmentConfig();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setApiId(entity.getApiId());
        domain.setEnvironment(toEnvironmentType(entity.getEnvironment()));
        domain.setBaseUrl(entity.getBaseUrl());
        domain.setEndpointPath(entity.getEndpointPath());
        domain.setCredentials(entity.getCredentials());
        domain.setHeaders(entity.getHeaders());
        domain.setQueryParams(entity.getQueryParams());
        domain.setAuthType(toAuthType(entity.getAuthType()));
        domain.setActive(entity.isActive());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setDeletedAt(toInstant(entity.getDeletedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    public ApiEnvironmentConfigJpaEntity toEntity(ApiEnvironmentConfig domain) {
        if (domain == null) return null;
        var entity = new ApiEnvironmentConfigJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setApiId(domain.getApiId());
        entity.setEnvironment(toEnvConfigEnvironmentTypeEnum(domain.getEnvironment()));
        entity.setBaseUrl(domain.getBaseUrl());
        entity.setEndpointPath(domain.getEndpointPath());
        entity.setCredentials(domain.getCredentials());
        entity.setHeaders(domain.getHeaders());
        entity.setQueryParams(domain.getQueryParams());
        entity.setAuthType(toEnvAuthTypeEnum(domain.getAuthType()));
        entity.setActive(domain.isActive());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setDeletedAt(toOffsetDateTime(domain.getDeletedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    // ==================== ApiClient ====================

    public ApiClient toDomain(ApiClientJpaEntity entity) {
        if (entity == null) return null;
        var domain = new ApiClient();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setName(entity.getName());
        domain.setCode(entity.getCode());
        domain.setDescription(entity.getDescription());
        domain.setSecretKey(entity.getSecretKey());
        domain.setCallbackUrl(entity.getCallbackUrl());
        domain.setIpWhitelist(entity.getIpWhitelist() != null
                ? Arrays.asList(entity.getIpWhitelist()) : List.of());
        domain.setStatus(toClientStatus(entity.getStatus()));
        domain.setEnvironment(toAccessEnvironment(entity.getEnvironment()));
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setDeletedAt(toInstant(entity.getDeletedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    public ApiClientJpaEntity toEntity(ApiClient domain) {
        if (domain == null) return null;
        var entity = new ApiClientJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setName(domain.getName());
        entity.setCode(domain.getCode());
        entity.setDescription(domain.getDescription());
        entity.setSecretKey(domain.getSecretKey());
        entity.setCallbackUrl(domain.getCallbackUrl());
        entity.setIpWhitelist(domain.getIpWhitelist() != null
                ? domain.getIpWhitelist().toArray(String[]::new) : null);
        entity.setStatus(toClientStatusEnum(domain.getStatus()));
        entity.setEnvironment(toAccessEnvironmentEnum(domain.getEnvironment()));
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setDeletedAt(toOffsetDateTime(domain.getDeletedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    // ==================== ApiRequestLog ====================

    public ApiRequestLog toDomain(ApiRequestLogJpaEntity entity) {
        if (entity == null) return null;
        var domain = new ApiRequestLog();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setApiId(entity.getApiId());
        domain.setClientId(entity.getClientId());
        domain.setRequestId(entity.getRequestId());
        domain.setEnvironment(toEnvironmentType(entity.getEnvironment()));
        domain.setHttpMethod(toHttpMethod(entity.getHttpMethod()));
        domain.setRequestUrl(entity.getRequestUrl());
        domain.setRequestHeaders(entity.getRequestHeaders());
        domain.setRequestBody(entity.getRequestBody());
        domain.setResponseStatus(entity.getResponseStatus());
        domain.setResponseHeaders(entity.getResponseHeaders());
        domain.setResponseBody(entity.getResponseBody());
        domain.setStatus(toRequestStatus(entity.getStatus()));
        domain.setDurationMs(entity.getDurationMs());
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setNationalId(entity.getNationalId());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        return domain;
    }

    public ApiRequestLogJpaEntity toEntity(ApiRequestLog domain) {
        if (domain == null) return null;
        var entity = new ApiRequestLogJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setApiId(domain.getApiId());
        entity.setClientId(domain.getClientId());
        entity.setRequestId(domain.getRequestId());
        entity.setEnvironment(toLogEnvironmentTypeEnum(domain.getEnvironment()));
        entity.setHttpMethod(toLogHttpMethodEnum(domain.getHttpMethod()));
        entity.setRequestUrl(domain.getRequestUrl());
        entity.setRequestHeaders(domain.getRequestHeaders());
        entity.setRequestBody(domain.getRequestBody());
        entity.setResponseStatus(domain.getResponseStatus());
        entity.setResponseHeaders(domain.getResponseHeaders());
        entity.setResponseBody(domain.getResponseBody());
        entity.setStatus(toRequestStatusEnum(domain.getStatus()));
        entity.setDurationMs(domain.getDurationMs());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setNationalId(domain.getNationalId());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        return entity;
    }

    // ==================== ClientRequest (env-specific tables) ====================

    public ClientRequest toDomain(ClientRequestBaseJpaEntity entity, EnvironmentType environment) {
        if (entity == null) return null;
        var domain = new ClientRequest();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setApiId(entity.getApiId());
        domain.setClientId(entity.getClientId());
        domain.setRequestId(entity.getRequestId());
        domain.setProviderCode(entity.getProviderCode());
        domain.setApiCode(entity.getApiCode());
        domain.setEnvironment(environment);
        domain.setHttpMethod(toClientRequestHttpMethod(entity.getHttpMethod()));
        domain.setRequestUrl(entity.getRequestUrl());
        domain.setRequestHeaders(entity.getRequestHeaders());
        domain.setRequestBody(entity.getRequestBody());
        domain.setResponseStatus(entity.getResponseStatus());
        domain.setResponseHeaders(entity.getResponseHeaders());
        domain.setResponseBody(entity.getResponseBody());
        domain.setStatus(toClientRequestStatus(entity.getStatus()));
        domain.setDurationMs(entity.getDurationMs());
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setNationalId(entity.getNationalId());
        domain.setMobileNumber(entity.getMobileNumber());
        domain.setCallerService(entity.getCallerService());
        domain.setCustomerId(entity.getCustomerId());
        domain.setApplicationId(entity.getApplicationId());
        domain.setContextType(entity.getContextType());
        domain.setApiCost(entity.getApiCost());
        domain.setCostCurrency(entity.getCostCurrency());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        return domain;
    }

    public void copyToEntity(ClientRequest domain, ClientRequestBaseJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setApiId(domain.getApiId());
        entity.setClientId(domain.getClientId());
        entity.setRequestId(domain.getRequestId());
        entity.setProviderCode(domain.getProviderCode());
        entity.setApiCode(domain.getApiCode());
        entity.setHttpMethod(toClientRequestHttpMethodEnum(domain.getHttpMethod()));
        entity.setRequestUrl(domain.getRequestUrl());
        entity.setRequestHeaders(domain.getRequestHeaders());
        entity.setRequestBody(domain.getRequestBody());
        entity.setResponseStatus(domain.getResponseStatus());
        entity.setResponseHeaders(domain.getResponseHeaders());
        entity.setResponseBody(domain.getResponseBody());
        entity.setStatus(toClientRequestStatusEnum(domain.getStatus()));
        entity.setDurationMs(domain.getDurationMs());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setNationalId(domain.getNationalId());
        entity.setMobileNumber(domain.getMobileNumber());
        entity.setCallerService(domain.getCallerService());
        entity.setCustomerId(domain.getCustomerId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setContextType(domain.getContextType());
        entity.setApiCost(domain.getApiCost());
        entity.setCostCurrency(domain.getCostCurrency());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
    }

    private HttpMethod toClientRequestHttpMethod(ClientRequestBaseJpaEntity.HttpMethodEnum e) {
        return e != null ? HttpMethod.valueOf(e.name()) : null;
    }

    private ClientRequestBaseJpaEntity.HttpMethodEnum toClientRequestHttpMethodEnum(HttpMethod m) {
        return m != null ? ClientRequestBaseJpaEntity.HttpMethodEnum.valueOf(m.name()) : null;
    }

    private RequestStatus toClientRequestStatus(ClientRequestBaseJpaEntity.RequestStatusEnum e) {
        return e != null ? RequestStatus.valueOf(e.name()) : null;
    }

    private ClientRequestBaseJpaEntity.RequestStatusEnum toClientRequestStatusEnum(RequestStatus s) {
        return s != null ? ClientRequestBaseJpaEntity.RequestStatusEnum.valueOf(s.name()) : null;
    }

    // ==================== CallbackResponse ====================

    public CallbackResponse toDomain(CallbackResponseJpaEntity entity) {
        if (entity == null) return null;
        var domain = new CallbackResponse();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setApiId(entity.getApiId());
        domain.setClientId(entity.getClientId());
        domain.setRequestLogId(entity.getRequestLogId());
        domain.setCallbackData(entity.getCallbackData());
        domain.setStatus(toCallbackStatus(entity.getStatus()));
        domain.setProcessedAt(toInstant(entity.getProcessedAt()));
        domain.setErrorMessage(entity.getErrorMessage());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        return domain;
    }

    public CallbackResponseJpaEntity toEntity(CallbackResponse domain) {
        if (domain == null) return null;
        var entity = new CallbackResponseJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setApiId(domain.getApiId());
        entity.setClientId(domain.getClientId());
        entity.setRequestLogId(domain.getRequestLogId());
        entity.setCallbackData(domain.getCallbackData());
        entity.setStatus(toCallbackStatusEnum(domain.getStatus()));
        entity.setProcessedAt(toOffsetDateTime(domain.getProcessedAt()));
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        return entity;
    }

    // ==================== Enum Conversions ====================

    private ProviderCategory toProviderCategory(ProviderCategoryEnum e) {
        return e != null ? ProviderCategory.valueOf(e.name()) : null;
    }
    private ProviderCategoryEnum toProviderCategoryEnum(ProviderCategory c) {
        return c != null ? ProviderCategoryEnum.valueOf(c.name()) : null;
    }

    private ProviderStatus toProviderStatus(ProviderStatusEnum e) {
        return e != null ? ProviderStatus.valueOf(e.name()) : null;
    }
    private ProviderStatusEnum toProviderStatusEnum(ProviderStatus s) {
        return s != null ? ProviderStatusEnum.valueOf(s.name()) : null;
    }

    private AuthType toAuthType(ThirdPartyProviderJpaEntity.AuthTypeEnum e) {
        return e != null ? AuthType.valueOf(e.name()) : null;
    }
    private ThirdPartyProviderJpaEntity.AuthTypeEnum toAuthTypeEnum(AuthType a) {
        return a != null ? ThirdPartyProviderJpaEntity.AuthTypeEnum.valueOf(a.name()) : null;
    }

    private AuthType toAuthType(ApiEnvironmentConfigJpaEntity.AuthTypeEnum e) {
        return e != null ? AuthType.valueOf(e.name()) : null;
    }
    private ApiEnvironmentConfigJpaEntity.AuthTypeEnum toEnvAuthTypeEnum(AuthType a) {
        return a != null ? ApiEnvironmentConfigJpaEntity.AuthTypeEnum.valueOf(a.name()) : null;
    }

    private ApiStatus toApiStatus(ProviderApiJpaEntity.ApiStatusEnum e) {
        return e != null ? ApiStatus.valueOf(e.name()) : null;
    }
    private ProviderApiJpaEntity.ApiStatusEnum toApiStatusEnum(ApiStatus s) {
        return s != null ? ProviderApiJpaEntity.ApiStatusEnum.valueOf(s.name()) : null;
    }

    private HttpMethod toHttpMethod(ProviderApiJpaEntity.HttpMethodEnum e) {
        return e != null ? HttpMethod.valueOf(e.name()) : null;
    }
    private ProviderApiJpaEntity.HttpMethodEnum toApiHttpMethodEnum(HttpMethod m) {
        return m != null ? ProviderApiJpaEntity.HttpMethodEnum.valueOf(m.name()) : null;
    }

    private HttpMethod toHttpMethod(ApiRequestLogJpaEntity.HttpMethodEnum e) {
        return e != null ? HttpMethod.valueOf(e.name()) : null;
    }
    private ApiRequestLogJpaEntity.HttpMethodEnum toLogHttpMethodEnum(HttpMethod m) {
        return m != null ? ApiRequestLogJpaEntity.HttpMethodEnum.valueOf(m.name()) : null;
    }

    private EnvironmentType toEnvironmentType(ApiEnvironmentConfigJpaEntity.EnvironmentTypeEnum e) {
        return e != null ? EnvironmentType.valueOf(e.name()) : null;
    }
    private ApiEnvironmentConfigJpaEntity.EnvironmentTypeEnum toEnvConfigEnvironmentTypeEnum(EnvironmentType t) {
        return t != null ? ApiEnvironmentConfigJpaEntity.EnvironmentTypeEnum.valueOf(t.name()) : null;
    }

    private EnvironmentType toEnvironmentType(ApiRequestLogJpaEntity.EnvironmentTypeEnum e) {
        return e != null ? EnvironmentType.valueOf(e.name()) : null;
    }
    private ApiRequestLogJpaEntity.EnvironmentTypeEnum toLogEnvironmentTypeEnum(EnvironmentType t) {
        return t != null ? ApiRequestLogJpaEntity.EnvironmentTypeEnum.valueOf(t.name()) : null;
    }

    private ClientStatus toClientStatus(ApiClientJpaEntity.ClientStatusEnum e) {
        return e != null ? ClientStatus.valueOf(e.name()) : null;
    }
    private ApiClientJpaEntity.ClientStatusEnum toClientStatusEnum(ClientStatus s) {
        return s != null ? ApiClientJpaEntity.ClientStatusEnum.valueOf(s.name()) : null;
    }

    private AccessEnvironment toAccessEnvironment(ApiClientJpaEntity.AccessEnvironmentEnum e) {
        return e != null ? AccessEnvironment.valueOf(e.name()) : null;
    }
    private ApiClientJpaEntity.AccessEnvironmentEnum toAccessEnvironmentEnum(AccessEnvironment a) {
        return a != null ? ApiClientJpaEntity.AccessEnvironmentEnum.valueOf(a.name()) : null;
    }

    private RequestStatus toRequestStatus(ApiRequestLogJpaEntity.RequestStatusEnum e) {
        return e != null ? RequestStatus.valueOf(e.name()) : null;
    }
    private ApiRequestLogJpaEntity.RequestStatusEnum toRequestStatusEnum(RequestStatus s) {
        return s != null ? ApiRequestLogJpaEntity.RequestStatusEnum.valueOf(s.name()) : null;
    }

    private CallbackStatus toCallbackStatus(CallbackResponseJpaEntity.CallbackStatusEnum e) {
        return e != null ? CallbackStatus.valueOf(e.name()) : null;
    }
    private CallbackResponseJpaEntity.CallbackStatusEnum toCallbackStatusEnum(CallbackStatus s) {
        return s != null ? CallbackResponseJpaEntity.CallbackStatusEnum.valueOf(s.name()) : null;
    }

    // ==================== Timestamp Conversions ====================

    private Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
