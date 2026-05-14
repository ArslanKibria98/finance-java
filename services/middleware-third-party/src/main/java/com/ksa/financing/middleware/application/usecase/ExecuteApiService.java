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
 *
 * Flow:
 *  1. Authenticate calling client by X-Secret-Key.
 *  2. Resolve client environment (TEST / DEV / PROD) and choose target table:
 *       TEST -> client_request_test  (mock response dispatched locally)
 *       DEV  -> client_request_dev   (live HTTP call using provider DEV creds)
 *       PROD -> client_request_prod  (live HTTP call using provider PROD creds)
 *  3. Idempotency lookup is scoped to the client's environment table.
 *  4. Persist the full request/response envelope in the env-specific table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteApiService implements ExecuteApiUseCase {

    private final ProviderApiRepository providerApiRepository;
    private final ProviderRepository providerRepository;
    private final EnvConfigRepository envConfigRepository;
    private final ClientRequestRepository clientRequestRepository;
    private final ClientRepository clientRepository;
    private final ClientAccessRepository clientAccessRepository;
    private final MockResponseDispatcher mockResponseDispatcher;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                                   Map<String, String> pathParams, Map<String, String> queryParams,
                                   Map<String, String> headers, String idempotencyKey,
                                   String nationalId, String mobileNumber, String callerService) {
        return execute(secretKey, apiCode, requestBody, pathParams, queryParams, headers,
                idempotencyKey, nationalId, mobileNumber, callerService, BusinessContext.empty());
    }

    @Override
    @Transactional
    public ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                                   Map<String, String> pathParams, Map<String, String> queryParams,
                                   Map<String, String> headers, String idempotencyKey,
                                   String nationalId, String mobileNumber, String callerService,
                                   BusinessContext context) {

        if (context == null) context = BusinessContext.empty();

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
        var environment = toEnvironmentType(client.getEnvironment());
        log.info("Executing API call: apiCode={}, env={}, tenant={}, client={}, caller={}",
                apiCode, environment, tenantId, client.getCode(), callerService);

        // Env-scoped idempotency: a TEST replay must not return a DEV/PROD result.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var cached = clientRequestRepository.findByIdempotencyKey(tenantId, environment, idempotencyKey);
            if (cached.isPresent()) {
                var existing = cached.get();
                if (existing.getStatus() == RequestStatus.SUCCESS || existing.getStatus() == RequestStatus.FAILED) {
                    log.info("Idempotent hit: env={}, key={}", environment, idempotencyKey);
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

        var providerApi = providerApiRepository.findByCode(tenantId, apiCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND, "API not found: " + apiCode));

        if (providerApi.getStatus() != ApiStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "API is not active: " + apiCode);
        }

        var apiAccess = clientAccessRepository.findApiAccess(tenantId, client.getId(), providerApi.getId());
        if (apiAccess.isEmpty() || !apiAccess.get().isActive()) {
            throw new BusinessException(ErrorCodes.ACCESS_DENIED,
                    "Client does not have access to API: " + apiCode);
        }

        var provider = providerRepository.findById(tenantId, providerApi.getProviderId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND, "Provider not found for API: " + apiCode));

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Provider is not active: " + provider.getCode());
        }

        if (environment == EnvironmentType.TEST) {
            return executeMock(tenantId, environment, client, provider, providerApi, apiCode,
                    requestBody, idempotencyKey, nationalId, mobileNumber, callerService, context);
        }
        return executeLive(tenantId, environment, client, provider, providerApi, apiCode,
                requestBody, pathParams, queryParams, headers, idempotencyKey,
                nationalId, mobileNumber, callerService, context);
    }

    private ExecutionResult executeMock(UUID tenantId, EnvironmentType environment, ApiClient client,
                                         ThirdPartyProvider provider, ProviderApi providerApi, String apiCode,
                                         String requestBody, String idempotencyKey,
                                         String nationalId, String mobileNumber, String callerService,
                                         BusinessContext context) {

        log.info("Mock execution: apiCode={}, provider={}, client={}", apiCode, provider.getCode(), client.getCode());

        String requestId = generateRequestId();
        var clientRequest = ClientRequest.create(
                tenantId, providerApi.getId(), client.getId(), requestId,
                provider.getCode(), apiCode, environment, providerApi.getHttpMethod(),
                "MOCK://" + apiCode, "{}", ensureJsonBody(requestBody),
                idempotencyKey, nationalId, mobileNumber, callerService,
                context.customerId(), context.applicationId(), context.contextType(),
                providerApi.getCostPerCall(), providerApi.getCostCurrency()
        );
        clientRequest = clientRequestRepository.save(clientRequest);

        var mockResult = mockResponseDispatcher.dispatch(provider.getCode(), apiCode, requestBody);
        String responseHeadersJson = normalizeHeadersToJson(mockResult.responseHeaders());
        String responseBodyJson = ensureJsonBody(mockResult.responseBody());

        clientRequest.markSuccess(mockResult.httpStatus(), responseHeadersJson, responseBodyJson, 0L);
        clientRequest = clientRequestRepository.save(clientRequest);

        log.info("Mock execution completed: apiCode={}, status={}, table={}",
                apiCode, mockResult.httpStatus(), targetTable(environment));

        return new ExecutionResult(
                requestId,
                mockResult.httpStatus(),
                mockResult.responseBody(),
                responseHeadersJson,
                0L,
                true,
                null
        );
    }

    private ExecutionResult executeLive(UUID tenantId, EnvironmentType environment, ApiClient client,
                                         ThirdPartyProvider provider, ProviderApi providerApi, String apiCode,
                                         String requestBody, Map<String, String> pathParams,
                                         Map<String, String> queryParams, Map<String, String> headers,
                                         String idempotencyKey, String nationalId,
                                         String mobileNumber, String callerService,
                                         BusinessContext context) {

        var envConfigs = envConfigRepository.findAllByApi(tenantId, providerApi.getId());
        var envConfig = envConfigs.stream()
                .filter(c -> c.getEnvironment() == environment && c.isActive())
                .findFirst()
                .orElse(null);

        String baseUrl = resolveBaseUrl(provider, envConfig, environment);
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
        var clientRequest = ClientRequest.create(
                tenantId, providerApi.getId(), client.getId(), requestId,
                provider.getCode(), apiCode, environment, providerApi.getHttpMethod(),
                finalUrl, httpHeadersToJson(httpHeaders), ensureJsonBody(requestBody),
                idempotencyKey, nationalId, mobileNumber, callerService,
                context.customerId(), context.applicationId(), context.contextType(),
                providerApi.getCostPerCall(), providerApi.getCostCurrency()
        );
        clientRequest = clientRequestRepository.save(clientRequest);

        long startTime = System.currentTimeMillis();
        try {
            var httpEntity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, httpMethod, httpEntity, String.class);
            long duration = System.currentTimeMillis() - startTime;

            String respHeadersJson = httpHeadersToJson(response.getHeaders());
            String respBodyJson = ensureJsonBody(response.getBody());
            clientRequest.markSuccess(response.getStatusCode().value(),
                    respHeadersJson, respBodyJson, duration);
            clientRequestRepository.save(clientRequest);

            log.info("Live API call succeeded: apiCode={}, env={}, status={}, duration={}ms",
                    apiCode, environment, response.getStatusCode().value(), duration);

            return new ExecutionResult(requestId, response.getStatusCode().value(),
                    response.getBody(), respHeadersJson, duration, true, null);

        } catch (HttpStatusCodeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            clientRequest.markFailed(ex.getStatusCode().value(),
                    ensureJsonBody(ex.getResponseBodyAsString()), ex.getMessage(), duration);
            clientRequestRepository.save(clientRequest);

            log.warn("Live API call failed: apiCode={}, env={}, status={}, duration={}ms",
                    apiCode, environment, ex.getStatusCode().value(), duration);

            return new ExecutionResult(requestId, ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(), null, duration, false, ex.getMessage());

        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            clientRequest.markTimeout(ex.getMessage(), duration);
            clientRequestRepository.save(clientRequest);

            log.error("Live API call timeout: apiCode={}, env={}, duration={}ms",
                    apiCode, environment, duration);

            return new ExecutionResult(requestId, 0, null, null, duration,
                    false, "Timeout: " + ex.getMessage());
        }
    }

    private EnvironmentType toEnvironmentType(AccessEnvironment access) {
        if (access == null) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Client environment is not set");
        }
        return switch (access) {
            case TEST -> EnvironmentType.TEST;
            case DEV, BOTH -> EnvironmentType.DEV;
            case PROD -> EnvironmentType.PROD;
        };
    }

    private String targetTable(EnvironmentType env) {
        return switch (env) {
            case TEST -> "client_request_test";
            case DEV -> "client_request_dev";
            case PROD -> "client_request_prod";
        };
    }

    /**
     * Serialize Spring HttpHeaders to a JSON object string suitable for a JSONB column.
     * Authorization / API-key values are redacted to avoid leaking secrets into audit logs.
     */
    private String httpHeadersToJson(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) return "{}";
        var map = new LinkedHashMap<String, Object>();
        headers.forEach((name, values) -> {
            boolean isSensitive = HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                    || "X-API-Key".equalsIgnoreCase(name)
                    || "Cookie".equalsIgnoreCase(name)
                    || "Set-Cookie".equalsIgnoreCase(name);
            if (isSensitive) {
                map.put(name, "***REDACTED***");
            } else if (values.size() == 1) {
                map.put(name, values.get(0));
            } else {
                map.put(name, values);
            }
        });
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Ensure the body string is valid JSON before persisting into a JSONB column.
     * Empty/null becomes {}. Non-JSON payloads (XML/HTML/text) are wrapped as {"raw":"..."}.
     */
    private String ensureJsonBody(String body) {
        if (body == null || body.isBlank()) return "{}";
        try {
            objectMapper.readTree(body);
            return body;
        } catch (Exception ignored) {
            try {
                return objectMapper.writeValueAsString(Map.of("raw", body));
            } catch (Exception e) {
                return "{}";
            }
        }
    }

    private String normalizeHeadersToJson(String headers) {
        if (headers == null || headers.isBlank()) return "{}";
        try {
            objectMapper.readTree(headers);
            return headers;
        } catch (Exception ignored) {
            var headerMap = new LinkedHashMap<String, String>();
            for (String line : headers.split("\n")) {
                var parts = line.split(":", 2);
                if (parts.length == 2) {
                    headerMap.put(parts[0].trim(), parts[1].trim());
                }
            }
            try {
                return objectMapper.writeValueAsString(headerMap);
            } catch (Exception e) {
                return "{}";
            }
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
                case BASIC -> headers.setBasicAuth(
                        creds.getOrDefault("username", ""),
                        creds.getOrDefault("password", ""));
                case BEARER -> headers.setBearerAuth(creds.getOrDefault("token", ""));
                case API_KEY -> headers.set(
                        creds.getOrDefault("headerName", "X-API-Key"),
                        creds.getOrDefault("apiKey", ""));
                case OAUTH2 -> {
                    String accessToken = creds.getOrDefault("accessToken", "");
                    if (!accessToken.isBlank()) {
                        headers.setBearerAuth(accessToken);
                    }
                }
                case CUSTOM -> creds.forEach(headers::set);
                case NONE -> { /* no auth */ }
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
