package com.ksa.financing.middleware.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.middleware.adapter.mock.MockResponseDispatcher;
import com.ksa.financing.middleware.domain.model.*;
import com.ksa.financing.middleware.domain.port.in.ExecuteApiUseCase;
import com.ksa.financing.middleware.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes third-party API calls through the middleware gateway.
 * Authenticates client via secretKey, verifies API access, then routes to mock or live.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteApiService implements ExecuteApiUseCase {

    private final ProviderApiRepository providerApiRepository;
    private final ProviderRepository providerRepository;
    private final EnvConfigRepository envConfigRepository;
    private final RequestLogRepository requestLogRepository;
    private final ClientRepository clientRepository;
    private final ClientAccessRepository clientAccessRepository;
    private final MockResponseDispatcher mockResponseDispatcher;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.environment:DEV}")
    private String activeEnvironment;

    @Override
    @Transactional
    public ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                                   Map<String, String> pathParams, Map<String, String> queryParams,
                                   Map<String, String> headers, String idempotencyKey,
                                   String nationalId, String callerService) {

        // 1. Authenticate client by secretKey
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Secret key is required");
        }

        var client = clientRepository.findBySecretKey(secretKey)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.INVALID_CREDENTIALS, "Invalid client secret key"));

        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Client is not active: " + client.getCode());
        }

        var tenantId = client.getTenantId();
        log.info("Executing API call: apiCode={}, tenant={}, client={}, caller={}",
                apiCode, tenantId, client.getCode(), callerService);

        // 2. Check idempotency — return cached result if already processed
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var cached = requestLogRepository.findByIdempotencyKey(tenantId, idempotencyKey);
            if (cached.isPresent()) {
                var existing = cached.get();
                if (existing.getStatus() == RequestStatus.SUCCESS || existing.getStatus() == RequestStatus.FAILED) {
                    log.info("Idempotent hit: returning cached result for key={}", idempotencyKey);
                    return new ExecutionResult(
                            existing.getRequestId(),
                            existing.getResponseStatus() != null ? existing.getResponseStatus() : 0,
                            existing.getResponseBody(),
                            existing.getResponseHeaders(),
                            existing.getDurationMs() != null ? existing.getDurationMs() : 0,
                            existing.getStatus() == RequestStatus.SUCCESS,
                            existing.getErrorMessage()
                    );
                }
            }
        }

        // 3. Resolve API definition
        var providerApi = providerApiRepository.findByCode(tenantId, apiCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND, "API not found: " + apiCode));

        if (providerApi.getStatus() != ApiStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "API is not active: " + apiCode);
        }

        // 4. Verify client has access to this API
        var apiAccess = clientAccessRepository.findApiAccess(tenantId, client.getId(), providerApi.getId());
        if (apiAccess.isEmpty() || !apiAccess.get().isActive()) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED,
                    "Client does not have access to API: " + apiCode);
        }

        // 5. Resolve provider
        var provider = providerRepository.findById(tenantId, providerApi.getProviderId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND, "Provider not found for API: " + apiCode));

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Provider is not active: " + provider.getCode());
        }

        // 6. Route by client environment: TEST → mock, DEV/PROD → live HTTP
        if (client.getEnvironment() == AccessEnvironment.TEST) {
            return executeMock(tenantId, client, provider, providerApi, apiCode,
                    requestBody, idempotencyKey, nationalId);
        }

        // 7. Live execution for DEV/PROD/BOTH clients
        return executeLive(tenantId, client, provider, providerApi, apiCode,
                requestBody, pathParams, queryParams, headers, idempotencyKey, nationalId);
    }

    private ExecutionResult executeMock(UUID tenantId, ApiClient client, ThirdPartyProvider provider,
                                         ProviderApi providerApi, String apiCode,
                                         String requestBody, String idempotencyKey, String nationalId) {

        log.info("Mock execution: apiCode={}, provider={}, client={}", apiCode, provider.getCode(), client.getCode());

        String requestId = generateRequestId();
        var requestLog = ApiRequestLog.create(
                tenantId, providerApi.getId(), client.getId(), requestId,
                EnvironmentType.DEV, providerApi.getHttpMethod(), "MOCK://" + apiCode,
                "{}", requestBody != null ? requestBody : "{}", idempotencyKey, nationalId
        );
        requestLog = requestLogRepository.save(requestLog);

        var mockResult = mockResponseDispatcher.dispatch(provider.getCode(), apiCode, requestBody);

        // Convert plain-text headers to valid JSON for JSONB column
        String responseHeadersJson = "{}";
        if (mockResult.responseHeaders() != null && !mockResult.responseHeaders().isBlank()) {
            try {
                // If already valid JSON, use as-is
                objectMapper.readTree(mockResult.responseHeaders());
                responseHeadersJson = mockResult.responseHeaders();
            } catch (Exception e) {
                // Convert "Key: Value" format to JSON object
                var headerMap = new java.util.LinkedHashMap<String, String>();
                for (String line : mockResult.responseHeaders().split("\n")) {
                    var parts = line.split(":", 2);
                    if (parts.length == 2) {
                        headerMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
                try {
                    responseHeadersJson = objectMapper.writeValueAsString(headerMap);
                } catch (Exception ignored) {
                    responseHeadersJson = "{}";
                }
            }
        }

        requestLog.markSuccess(
                mockResult.httpStatus(),
                responseHeadersJson,
                mockResult.responseBody(),
                0L
        );
        requestLogRepository.save(requestLog);

        log.info("Mock execution completed: apiCode={}, status={}", apiCode, mockResult.httpStatus());

        return new ExecutionResult(
                requestId,
                mockResult.httpStatus(),
                mockResult.responseBody(),
                mockResult.responseHeaders(),
                0L,
                true,
                null
        );
    }

    private ExecutionResult executeLive(UUID tenantId, ApiClient client, ThirdPartyProvider provider,
                                         ProviderApi providerApi, String apiCode,
                                         String requestBody, Map<String, String> pathParams,
                                         Map<String, String> queryParams, Map<String, String> headers,
                                         String idempotencyKey, String nationalId) {

        var envType = EnvironmentType.valueOf(activeEnvironment);
        var envConfigs = envConfigRepository.findAllByApi(tenantId, providerApi.getId());
        var envConfig = envConfigs.stream()
                .filter(c -> c.getEnvironment() == envType && c.isActive())
                .findFirst()
                .orElse(null);

        String baseUrl = resolveBaseUrl(provider, envConfig, envType);
        String endpointPath = envConfig != null && envConfig.getEndpointPath() != null
                ? envConfig.getEndpointPath()
                : providerApi.getEndpointPath();

        if (pathParams != null) {
            for (var entry : pathParams.entrySet()) {
                endpointPath = endpointPath.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        String fullUrl = baseUrl + endpointPath;

        var uriBuilder = UriComponentsBuilder.fromHttpUrl(fullUrl);
        if (envConfig != null && envConfig.getQueryParams() != null) {
            mergeQueryParams(uriBuilder, envConfig.getQueryParams());
        }
        if (queryParams != null) {
            queryParams.forEach(uriBuilder::queryParam);
        }
        String finalUrl = uriBuilder.build().toUriString();

        var httpHeaders = buildHeaders(provider, envConfig, headers);
        var httpMethod = resolveHttpMethod(providerApi.getHttpMethod());

        String requestId = generateRequestId();
        var requestLog = ApiRequestLog.create(
                tenantId, providerApi.getId(), client.getId(), requestId,
                envType, providerApi.getHttpMethod(), finalUrl,
                sanitizeHeaders(httpHeaders.toString()), requestBody,
                idempotencyKey, nationalId
        );
        requestLog = requestLogRepository.save(requestLog);

        long startTime = System.currentTimeMillis();
        try {
            var httpEntity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, httpMethod, httpEntity, String.class);

            long duration = System.currentTimeMillis() - startTime;

            requestLog.markSuccess(
                    response.getStatusCode().value(),
                    response.getHeaders().toString(),
                    response.getBody(),
                    duration
            );
            requestLogRepository.save(requestLog);

            log.info("API call succeeded: apiCode={}, status={}, duration={}ms",
                    apiCode, response.getStatusCode().value(), duration);

            return new ExecutionResult(
                    requestId,
                    response.getStatusCode().value(),
                    response.getBody(),
                    response.getHeaders().toString(),
                    duration,
                    true,
                    null
            );

        } catch (HttpStatusCodeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            requestLog.markFailed(
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex.getMessage(),
                    duration
            );
            requestLogRepository.save(requestLog);

            log.warn("API call failed: apiCode={}, status={}, duration={}ms, error={}",
                    apiCode, ex.getStatusCode().value(), duration, ex.getMessage());

            return new ExecutionResult(
                    requestId,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    null,
                    duration,
                    false,
                    ex.getMessage()
            );

        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            requestLog.markTimeout(ex.getMessage(), duration);
            requestLogRepository.save(requestLog);

            log.error("API call timeout: apiCode={}, duration={}ms, error={}",
                    apiCode, duration, ex.getMessage());

            return new ExecutionResult(
                    requestId,
                    0,
                    null,
                    null,
                    duration,
                    false,
                    "Timeout: " + ex.getMessage()
            );
        }
    }

    private String resolveBaseUrl(ThirdPartyProvider provider, ApiEnvironmentConfig envConfig,
                                   EnvironmentType envType) {
        if (envConfig != null && envConfig.getBaseUrl() != null && !envConfig.getBaseUrl().isBlank()) {
            return envConfig.getBaseUrl();
        }
        return envType == EnvironmentType.PROD ? provider.getBaseUrlProd() : provider.getBaseUrlDev();
    }

    private org.springframework.http.HttpMethod resolveHttpMethod(com.ksa.financing.middleware.domain.model.HttpMethod method) {
        return switch (method) {
            case GET -> org.springframework.http.HttpMethod.GET;
            case POST -> org.springframework.http.HttpMethod.POST;
            case PUT -> org.springframework.http.HttpMethod.PUT;
            case PATCH -> org.springframework.http.HttpMethod.PATCH;
            case DELETE -> org.springframework.http.HttpMethod.DELETE;
        };
    }

    private HttpHeaders buildHeaders(ThirdPartyProvider provider, ApiEnvironmentConfig envConfig,
                                     Map<String, String> additionalHeaders) {
        var httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));

        if (envConfig != null && envConfig.getHeaders() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> configHeaders = objectMapper.readValue(envConfig.getHeaders(), Map.class);
                configHeaders.forEach(httpHeaders::set);
            } catch (Exception e) {
                log.warn("Failed to parse env config headers: {}", e.getMessage());
            }
        }

        if (envConfig != null && envConfig.getCredentials() != null) {
            applyAuth(httpHeaders, envConfig.getAuthType() != null ? envConfig.getAuthType() : provider.getAuthType(),
                    envConfig.getCredentials());
        }

        if (additionalHeaders != null) {
            additionalHeaders.forEach(httpHeaders::set);
        }

        return httpHeaders;
    }

    private void applyAuth(HttpHeaders headers, AuthType authType, String credentialsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> creds = objectMapper.readValue(credentialsJson, Map.class);

            switch (authType) {
                case BASIC -> {
                    String username = creds.getOrDefault("username", "");
                    String password = creds.getOrDefault("password", "");
                    headers.setBasicAuth(username, password);
                }
                case BEARER -> {
                    String token = creds.getOrDefault("token", "");
                    headers.setBearerAuth(token);
                }
                case API_KEY -> {
                    String headerName = creds.getOrDefault("headerName", "X-API-Key");
                    String apiKey = creds.getOrDefault("apiKey", "");
                    headers.set(headerName, apiKey);
                }
                case OAUTH2 -> {
                    String accessToken = creds.getOrDefault("accessToken", "");
                    if (!accessToken.isBlank()) {
                        headers.setBearerAuth(accessToken);
                    }
                }
                case CUSTOM -> creds.forEach(headers::set);
                case NONE -> { /* No auth */ }
            }
        } catch (Exception e) {
            log.warn("Failed to apply auth credentials: {}", e.getMessage());
        }
    }

    private void mergeQueryParams(UriComponentsBuilder builder, String queryParamsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> params = objectMapper.readValue(queryParamsJson, Map.class);
            params.forEach(builder::queryParam);
        } catch (Exception e) {
            log.warn("Failed to parse query params: {}", e.getMessage());
        }
    }

    private String sanitizeHeaders(String headers) {
        return headers.replaceAll("(Authorization=\\[)([^]]*)(])", "$1***REDACTED***$3")
                .replaceAll("(X-API-Key=\\[)([^]]*)(])", "$1***REDACTED***$3");
    }

    private String generateRequestId() {
        long numericPart = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        return "Req-" + numericPart;
    }
}
