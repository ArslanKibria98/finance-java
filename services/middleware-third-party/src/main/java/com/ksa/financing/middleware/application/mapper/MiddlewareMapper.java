package com.ksa.financing.middleware.application.mapper;

import com.ksa.financing.middleware.application.dto.*;
import com.ksa.financing.middleware.domain.model.*;
import org.springframework.stereotype.Component;

@Component
public class MiddlewareMapper {

    public ProviderEnvironmentResponse toEnvironmentResponse(ThirdPartyProvider provider,
                                                               java.util.List<ProviderApiWithEnvConfigsResponse> apis) {
        return new ProviderEnvironmentResponse(
                provider.getId(),
                provider.getCode(),
                provider.getNameEn(),
                provider.getNameAr(),
                provider.getDescriptionEn(),
                provider.getDescriptionAr(),
                provider.getCategory() != null ? provider.getCategory().name() : null,
                provider.getBaseUrlDev(),
                provider.getBaseUrlProd(),
                provider.getAuthType() != null ? provider.getAuthType().name() : null,
                provider.getStatus() != null ? provider.getStatus().name() : null,
                provider.getTimeoutMs(),
                provider.getRetryCount(),
                provider.getCreatedAt(),
                provider.getUpdatedAt(),
                apis
        );
    }

    /**
     * Overload taking raw domain lists. Groups env configs by api_id then
     * delegates to the per-api builder so callers do not have to pre-group.
     */
    public ProviderEnvironmentResponse toEnvironmentResponse(ThirdPartyProvider provider,
                                                              java.util.List<ProviderApi> apis,
                                                              java.util.List<ApiEnvironmentConfig> envConfigs) {
        var configsByApi = envConfigs.stream()
                .collect(java.util.stream.Collectors.groupingBy(ApiEnvironmentConfig::getApiId));
        var apiResponses = apis.stream()
                .map(api -> toApiWithEnvConfigsResponse(
                        api,
                        configsByApi.getOrDefault(api.getId(), java.util.List.of())
                                .stream().map(this::toResponse).toList()))
                .toList();
        return toEnvironmentResponse(provider, apiResponses);
    }

    public ProviderApiWithEnvConfigsResponse toApiWithEnvConfigsResponse(ProviderApi api,
                                                                          java.util.List<EnvConfigResponse> envConfigs) {
        return new ProviderApiWithEnvConfigsResponse(
                api.getId(),
                api.getProviderId(),
                api.getCode(),
                api.getNameEn(),
                api.getNameAr(),
                api.getDescriptionEn(),
                api.getDescriptionAr(),
                api.getHttpMethod() != null ? api.getHttpMethod().name() : null,
                api.getEndpointPath(),
                api.getStatus() != null ? api.getStatus().name() : null,
                api.isAsync(),
                api.getTimeoutMs(),
                api.getCreatedAt(),
                api.getUpdatedAt(),
                envConfigs
        );
    }

    public ProviderResponse toResponse(ThirdPartyProvider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getCode(),
                provider.getNameEn(),
                provider.getNameAr(),
                provider.getDescriptionEn(),
                provider.getDescriptionAr(),
                provider.getCategory() != null ? provider.getCategory().name() : null,
                provider.getBaseUrlDev(),
                provider.getBaseUrlProd(),
                provider.getAuthType() != null ? provider.getAuthType().name() : null,
                provider.getStatus() != null ? provider.getStatus().name() : null,
                provider.getTimeoutMs(),
                provider.getRetryCount(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }

    public ProviderApiResponse toResponse(ProviderApi api) {
        return new ProviderApiResponse(
                api.getId(),
                api.getProviderId(),
                api.getCode(),
                api.getNameEn(),
                api.getNameAr(),
                api.getDescriptionEn(),
                api.getDescriptionAr(),
                api.getHttpMethod() != null ? api.getHttpMethod().name() : null,
                api.getEndpointPath(),
                api.getStatus() != null ? api.getStatus().name() : null,
                api.isAsync(),
                api.getTimeoutMs(),
                api.getCostPerCall(),
                api.getCostCurrency(),
                api.getCreatedAt(),
                api.getUpdatedAt()
        );
    }

    public EnvConfigResponse toResponse(ApiEnvironmentConfig config) {
        return new EnvConfigResponse(
                config.getId(),
                config.getApiId(),
                config.getEnvironment() != null ? config.getEnvironment().name() : null,
                config.getBaseUrl(),
                config.getEndpointPath(),
                config.getCredentials(),
                config.getHeaders(),
                config.getQueryParams(),
                config.getAuthType() != null ? config.getAuthType().name() : null,
                config.isActive(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    public ClientResponse toResponse(ApiClient client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getCode(),
                client.getDescription(),
                client.getSecretKey(),
                client.getCallbackUrl(),
                client.getIpWhitelist(),
                client.getStatus() != null ? client.getStatus().name() : null,
                client.getEnvironment() != null ? client.getEnvironment().name() : null,
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }

    public RequestLogResponse toResponse(ApiRequestLog log) {
        return new RequestLogResponse(
                log.getId(),
                log.getApiId(),
                log.getClientId(),
                log.getRequestId(),
                log.getEnvironment() != null ? log.getEnvironment().name() : null,
                log.getHttpMethod() != null ? log.getHttpMethod().name() : null,
                log.getRequestUrl(),
                log.getResponseStatus(),
                log.getStatus() != null ? log.getStatus().name() : null,
                log.getDurationMs(),
                log.getErrorMessage(),
                log.getIdempotencyKey(),
                log.getCreatedAt()
        );
    }

    public CallbackResponseDto toResponse(CallbackResponse callback) {
        return new CallbackResponseDto(
                callback.getId(),
                callback.getApiId(),
                callback.getClientId(),
                callback.getRequestLogId(),
                callback.getCallbackData(),
                callback.getStatus() != null ? callback.getStatus().name() : null,
                callback.getProcessedAt(),
                callback.getErrorMessage(),
                callback.getCreatedAt()
        );
    }
}
