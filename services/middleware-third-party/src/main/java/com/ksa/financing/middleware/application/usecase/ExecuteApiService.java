package com.ksa.financing.middleware.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.middleware.adapter.mock.MockResponseDispatcher;
import com.ksa.financing.middleware.application.audit.ClientRequestAuditEmitter;
import com.ksa.financing.middleware.domain.model.*;
import com.ksa.financing.middleware.domain.port.in.ExecuteApiUseCase;
import com.ksa.financing.middleware.domain.port.out.*;
import com.ksa.financing.middleware.infrastructure.crypto.JwsSignatureUtil;
import com.ksa.financing.middleware.infrastructure.template.JsonTemplateRenderer;
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
    private final ClientRequestAuditEmitter auditEmitter;
    private final com.ksa.financing.middleware.infrastructure.settlement.WalletSettlementClient walletSettlementClient;

    @Override
    public ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                                   Map<String, String> pathParams, Map<String, String> queryParams,
                                   Map<String, String> headers, String idempotencyKey,
                                   String nationalId, String mobileNumber, String callerService) {
        return execute(secretKey, apiCode, requestBody, pathParams, queryParams, headers,
                idempotencyKey, nationalId, mobileNumber, callerService, BusinessContext.empty());
    }

    // NOT @Transactional: this method makes a long-running external HTTP call (multipart
    // uploads can take 60s+ over the tunnel). Holding a DB tx open across the call trips
    // Postgres idle-in-transaction-timeout and kills the connection. Each clientRequest
    // save() commits in its own short tx instead — connection is never held during I/O.
    @Override
    public ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                                   Map<String, String> pathParams, Map<String, String> queryParams,
                                   Map<String, String> headers, String idempotencyKey,
                                   String nationalId, String mobileNumber, String callerService,
                                   BusinessContext context) {

        if (context == null) context = BusinessContext.empty();

        var rc = resolve(secretKey, apiCode);
        var tenantId = rc.tenantId();
        var environment = rc.environment();
        log.info("Executing API call: apiCode={}, env={}, tenant={}, client={}, caller={}",
                apiCode, environment, tenantId, rc.client().getCode(), callerService);

        // Keep the caller's ORIGINAL flat body (before template expansion) so a post-execution
        // wallet settlement can read debtorAccount / creditorAccount / amount.
        String callerBody = requestBody;

        // PRE-RAIL: for wallet-settlement APIs, validate the debit (debtor present + funded) BEFORE
        // calling the rail. If invalid (missing debtor / insufficient funds) this throws and the
        // caller gets the error — Scotia is never called and no money moves.
        walletSettlementClient.validateBeforeRail(apiCode, callerBody, tenantId);

        // If the API declares a request_template, the caller sends a SIMPLE flat body and the
        // middleware expands it into the exact provider body here (env-independent). Done before
        // idempotency/dispatch so the persisted + signed body is the full provider payload.
        requestBody = applyRequestTemplate(rc.providerApi(), requestBody);

        // Env-scoped idempotency: a TEST replay must not return a DEV/PROD result.
        var cachedResult = idempotentReplay(tenantId, environment, idempotencyKey);
        if (cachedResult != null) return cachedResult;

        ExecutionResult result = (environment == EnvironmentType.TEST)
                ? executeMock(tenantId, environment, rc.client(), rc.provider(), rc.providerApi(), apiCode,
                    requestBody, idempotencyKey, nationalId, mobileNumber, callerService, context)
                : executeLive(tenantId, environment, rc.client(), rc.provider(), rc.providerApi(), apiCode,
                    requestBody, pathParams, queryParams, headers, idempotencyKey,
                    nationalId, mobileNumber, callerService, context);

        // Post-execution: if this is a wallet-settlement API (e.g. Scotia payment commit) and it
        // succeeded, mirror the money into our wallets (debit debtor + credit creditor).
        walletSettlementClient.settleIfApplicable(apiCode, callerBody, tenantId, result);

        return result;
    }

    @Override
    public ExecutionResult executeMultipart(String secretKey, String apiCode, MultipartPart file,
                                            Map<String, String> pathParams, Map<String, String> queryParams,
                                            Map<String, String> headers, String idempotencyKey,
                                            String nationalId, String mobileNumber, String callerService,
                                            BusinessContext context) {

        if (context == null) context = BusinessContext.empty();
        if (file == null || file.content() == null || file.content().length == 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Multipart file is required for: " + apiCode);
        }

        var rc = resolve(secretKey, apiCode);
        var tenantId = rc.tenantId();
        var environment = rc.environment();
        log.info("Executing multipart API call: apiCode={}, env={}, file={} ({} bytes), client={}, caller={}",
                apiCode, environment, file.fileName(), file.content().length, rc.client().getCode(), callerService);

        var cachedResult = idempotentReplay(tenantId, environment, idempotencyKey);
        if (cachedResult != null) return cachedResult;

        // TEST: file is irrelevant — the mock provider answers from the apiCode alone.
        if (environment == EnvironmentType.TEST) {
            return executeMock(tenantId, environment, rc.client(), rc.provider(), rc.providerApi(), apiCode,
                    multipartAuditBody(file, queryParams), idempotencyKey, nationalId, mobileNumber,
                    callerService, context);
        }
        return executeLiveMultipart(tenantId, environment, rc.client(), rc.provider(), rc.providerApi(), apiCode,
                file, pathParams, queryParams, headers, idempotencyKey,
                nationalId, mobileNumber, callerService, context);
    }

    /**
     * Resolve and validate the calling client + provider + API for a request.
     * Pure read-only lookups shared by {@link #execute} and {@link #executeMultipart}.
     */
    private ResolvedCall resolve(String secretKey, String apiCode) {
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

        return new ResolvedCall(client, tenantId, environment, providerApi, provider);
    }

    /** Returns a cached result if an idempotent replay is found, otherwise null. */
    private ExecutionResult idempotentReplay(UUID tenantId, EnvironmentType environment, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        var cached = clientRequestRepository.findByIdempotencyKey(tenantId, environment, idempotencyKey);
        if (cached.isEmpty()) return null;
        var existing = cached.get();
        if (existing.getStatus() != RequestStatus.SUCCESS && existing.getStatus() != RequestStatus.FAILED) {
            return null;
        }
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

    private record ResolvedCall(ApiClient client, UUID tenantId, EnvironmentType environment,
                                ProviderApi providerApi, ThirdPartyProvider provider) {}

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
        auditEmitter.emit(clientRequest);

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

        // Mock-mode: a DEV/PROD env config can opt to be served by the local mock provider
        // (credentials.mockMode=true) instead of a live HTTP call — useful when a sandbox
        // is not reachable server-to-server. The response is still persisted into the
        // env-specific table (e.g. client_request_dev). Flip mockMode off once real
        // credentials/endpoint are available to switch to live calls.
        if (isMockMode(envConfig)) {
            log.info("Mock-mode env config — serving apiCode={} from local mock provider (env={})", apiCode, environment);
            return executeMock(tenantId, environment, client, provider, providerApi, apiCode,
                    requestBody, idempotencyKey, nationalId, mobileNumber, callerService, context);
        }

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

        // If the caller sent no body (or just {}), and the env config has credentials,
        // build the outgoing body from those credentials. This keeps sensitive client
        // credentials inside the middleware — callers don't need to pass them.
        String effectiveBody = injectCredentialsIfEmpty(requestBody, envConfig);

        // Resolve per-request dynamic header tokens ({{TRACE_ID}}, {{SPAN_ID}}, {{JWS}})
        // before the request is persisted, so the audit row shows the values actually sent.
        resolveDynamicHeaders(httpHeaders, effectiveBody, envConfig);

        String requestId = generateRequestId();
        var clientRequest = ClientRequest.create(
                tenantId, providerApi.getId(), client.getId(), requestId,
                provider.getCode(), apiCode, environment, providerApi.getHttpMethod(),
                finalUrl, httpHeadersToJson(httpHeaders), ensureJsonBody(effectiveBody),
                idempotencyKey, nationalId, mobileNumber, callerService,
                context.customerId(), context.applicationId(), context.contextType(),
                providerApi.getCostPerCall(), providerApi.getCostCurrency()
        );
        clientRequest = clientRequestRepository.save(clientRequest);

        long startTime = System.currentTimeMillis();
        try {
            // Providers like Twilio require application/x-www-form-urlencoded. When the
            // env config forces that content type, convert the JSON body to To=..&From=..
            // form-encoded. JSON providers (Facia, ANB, ...) are unaffected.
            String outgoingBody = encodeBodyForContentType(effectiveBody, httpHeaders);
            var httpEntity = new HttpEntity<>(outgoingBody, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, httpMethod, httpEntity, String.class);
            long duration = System.currentTimeMillis() - startTime;

            String respHeadersJson = httpHeadersToJson(response.getHeaders());
            String rawBody = response.getBody();
            // Provider envelopes (e.g. Facia wraps everything in `result.data`) are
            // unwrapped before returning so callers see a clean payload.
            String respBodyJson = unwrapProviderEnvelope(provider.getCode(), rawBody);
            clientRequest.markSuccess(response.getStatusCode().value(),
                    respHeadersJson, ensureJsonBody(rawBody), duration);
            clientRequest = clientRequestRepository.save(clientRequest);
            auditEmitter.emit(clientRequest);

            log.info("Live API call succeeded: apiCode={}, env={}, status={}, duration={}ms",
                    apiCode, environment, response.getStatusCode().value(), duration);

            return new ExecutionResult(requestId, response.getStatusCode().value(),
                    respBodyJson, respHeadersJson, duration, true, null);

        } catch (HttpStatusCodeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            String rawErrorBody = ex.getResponseBodyAsString();
            clientRequest.markFailed(ex.getStatusCode().value(),
                    ensureJsonBody(rawErrorBody), ex.getMessage(), duration);
            clientRequest = clientRequestRepository.save(clientRequest);
            auditEmitter.emit(clientRequest);

            log.warn("Live API call failed: apiCode={}, env={}, status={}, duration={}ms",
                    apiCode, environment, ex.getStatusCode().value(), duration);

            // Unwrap error body too so callers see the inner Facia error fields cleanly.
            String cleanErrorBody = unwrapProviderEnvelope(provider.getCode(), rawErrorBody);
            return new ExecutionResult(requestId, ex.getStatusCode().value(),
                    cleanErrorBody, null, duration, false, ex.getMessage());

        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            clientRequest.markTimeout(ex.getMessage(), duration);
            clientRequest = clientRequestRepository.save(clientRequest);
            auditEmitter.emit(clientRequest);

            log.error("Live API call timeout: apiCode={}, env={}, duration={}ms",
                    apiCode, environment, duration);

            return new ExecutionResult(requestId, 0, null, null, duration,
                    false, "Timeout: " + ex.getMessage());
        }
    }

    private ExecutionResult executeLiveMultipart(UUID tenantId, EnvironmentType environment, ApiClient client,
                                                 ThirdPartyProvider provider, ProviderApi providerApi, String apiCode,
                                                 MultipartPart file, Map<String, String> pathParams,
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

        var httpHeaders = buildMultipartHeaders(provider, envConfig, headers);

        // Build the multipart body: the file part keyed by its form field name.
        var body = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        var fileResource = new org.springframework.core.io.ByteArrayResource(file.content()) {
            @Override
            public String getFilename() {
                return file.fileName() != null ? file.fileName() : "upload";
            }
        };
        var partHeaders = new HttpHeaders();
        if (file.contentType() != null && !file.contentType().isBlank()) {
            try {
                partHeaders.setContentType(MediaType.parseMediaType(file.contentType()));
            } catch (Exception ignored) { /* let the converter infer it */ }
        }
        var filePart = new HttpEntity<>(fileResource, partHeaders);
        body.add(file.formField() != null ? file.formField() : "file", filePart);

        String requestId = generateRequestId();
        var clientRequest = ClientRequest.create(
                tenantId, providerApi.getId(), client.getId(), requestId,
                provider.getCode(), apiCode, environment, providerApi.getHttpMethod(),
                finalUrl, httpHeadersToJson(httpHeaders), multipartAuditBody(file, queryParams),
                idempotencyKey, nationalId, mobileNumber, callerService,
                context.customerId(), context.applicationId(), context.contextType(),
                providerApi.getCostPerCall(), providerApi.getCostCurrency()
        );
        clientRequest = clientRequestRepository.save(clientRequest);

        long startTime = System.currentTimeMillis();
        try {
            var httpEntity = new HttpEntity<>(body, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(
                    finalUrl, resolveHttpMethod(providerApi.getHttpMethod()), httpEntity, String.class);
            long duration = System.currentTimeMillis() - startTime;

            String respHeadersJson = httpHeadersToJson(response.getHeaders());
            String rawBody = response.getBody();
            String respBodyJson = unwrapProviderEnvelope(provider.getCode(), rawBody);
            clientRequest.markSuccess(response.getStatusCode().value(),
                    respHeadersJson, ensureJsonBody(rawBody), duration);
            clientRequest = clientRequestRepository.save(clientRequest);
            auditEmitter.emit(clientRequest);

            log.info("Live multipart call succeeded: apiCode={}, env={}, status={}, duration={}ms",
                    apiCode, environment, response.getStatusCode().value(), duration);

            return new ExecutionResult(requestId, response.getStatusCode().value(),
                    respBodyJson, respHeadersJson, duration, true, null);

        } catch (HttpStatusCodeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            String rawErrorBody = ex.getResponseBodyAsString();
            clientRequest.markFailed(ex.getStatusCode().value(),
                    ensureJsonBody(rawErrorBody), ex.getMessage(), duration);
            clientRequest = clientRequestRepository.save(clientRequest);
            auditEmitter.emit(clientRequest);

            log.warn("Live multipart call failed: apiCode={}, env={}, status={}, duration={}ms",
                    apiCode, environment, ex.getStatusCode().value(), duration);

            String cleanErrorBody = unwrapProviderEnvelope(provider.getCode(), rawErrorBody);
            return new ExecutionResult(requestId, ex.getStatusCode().value(),
                    cleanErrorBody, null, duration, false, ex.getMessage());

        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            clientRequest.markTimeout(ex.getMessage(), duration);
            clientRequest = clientRequestRepository.save(clientRequest);
            auditEmitter.emit(clientRequest);

            log.error("Live multipart call timeout: apiCode={}, env={}, duration={}ms",
                    apiCode, environment, duration);

            return new ExecutionResult(requestId, 0, null, null, duration,
                    false, "Timeout: " + ex.getMessage());
        }
    }

    /**
     * A JSON descriptor persisted in the audit tables in place of the binary file —
     * we never store the raw bytes, only metadata + the query params used.
     */
    private String multipartAuditBody(MultipartPart file, Map<String, String> queryParams) {
        var map = new LinkedHashMap<String, Object>();
        map.put("_multipart", true);
        map.put("formField", file.formField());
        map.put("fileName", file.fileName());
        map.put("sizeBytes", file.content() != null ? file.content().length : 0);
        map.put("contentType", file.contentType());
        if (queryParams != null && !queryParams.isEmpty()) {
            map.put("query", queryParams);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"_multipart\":true}";
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
                    || "Sullis-Api-Key".equalsIgnoreCase(name)
                    || "x-jws-signature".equalsIgnoreCase(name)
                    || "client-secret".equalsIgnoreCase(name)
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

    /**
     * If the caller sent no body (null / blank / {@code "{}"}) and the env config has
     * credentials, builds the outgoing body from those credentials. JSONB keys are
     * translated from camelCase to snake_case so {@code {clientId, clientSecret}}
     * becomes {@code {client_id, client_secret}} (standard for OAuth/auth-token APIs
     * like Facia's {@code /request-access-token}).
     *
     * <p>If caller sent a real body, returns it unchanged.</p>
     */
    /**
     * Strip the provider-specific response envelope so callers see a clean payload.
     *
     * <p>Facia wraps every successful response in {@code { status, message, result: { data: {...} } }}.
     * We unwrap to the innermost {@code data} object so callers can read fields like
     * {@code token}, {@code reference_id}, {@code similarity_score} directly at the top
     * of {@code responseBody}.</p>
     *
     * <p>Audit columns ({@code response_body} in {@code client_request_*}) still keep the
     * original raw body — only the {@code ExecuteApiResponse.responseBody} field returned
     * to the caller is unwrapped.</p>
     */
    @SuppressWarnings("unchecked")
    private String unwrapProviderEnvelope(String providerCode, String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return rawBody;
        // Only Facia has this nested shape today. Add more providers here as they come on board.
        if (!"FACIA".equalsIgnoreCase(providerCode)) return rawBody;
        try {
            Map<String, Object> envelope = objectMapper.readValue(rawBody, Map.class);
            Object resultNode = envelope.get("result");
            if (resultNode instanceof Map<?, ?> resultMap) {
                Object dataNode = ((Map<String, Object>) resultMap).get("data");
                if (dataNode != null) {
                    return objectMapper.writeValueAsString(dataNode);
                }
                return objectMapper.writeValueAsString(resultMap);
            }
            return rawBody;
        } catch (Exception e) {
            // Body wasn't JSON or didn't match envelope shape — return as-is.
            return rawBody;
        }
    }

    @SuppressWarnings("unchecked")
    private String injectCredentialsIfEmpty(String requestBody, ApiEnvironmentConfig envConfig) {
        boolean isEmpty = requestBody == null
                || requestBody.isBlank()
                || "{}".equals(requestBody.trim());
        if (!isEmpty) return requestBody;
        if (envConfig == null || envConfig.getCredentials() == null
                || envConfig.getCredentials().isBlank()) {
            return requestBody;
        }
        try {
            Map<String, String> creds = objectMapper.readValue(envConfig.getCredentials(), Map.class);
            // API_KEY providers carry header credentials ({headerName, apiKey}) and JWS
            // signing keys ({jwsPrivateKey}), NOT a body payload — injecting them would
            // leak the secret into the request body + audit.
            if (creds.containsKey("headerName") || creds.containsKey("apiKey")
                    || creds.containsKey("jwsPrivateKey") || creds.containsKey("mockMode")) {
                return requestBody;
            }
            Map<String, String> body = new java.util.LinkedHashMap<>();
            for (var entry : creds.entrySet()) {
                String key = entry.getKey();
                String snake = key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
                body.put(snake, entry.getValue());
            }
            String built = objectMapper.writeValueAsString(body);
            log.debug("Injected credentials-derived body (caller sent empty): keys={}", body.keySet());
            return built;
        } catch (Exception e) {
            log.warn("Failed to build body from credentials: {}", e.getMessage());
            return requestBody;
        }
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
            additionalHeaders.forEach((name, value) -> {
                if (isForwardableHeader(name)) {
                    httpHeaders.set(name, value);
                } else {
                    log.warn("Ignoring caller attempt to override protected header: {}", name);
                }
            });
        }
        return httpHeaders;
    }

    /**
     * Headers a caller is NEVER allowed to set/override via {@code X-Forward-Headers} — these carry
     * the middleware's managed credentials and provider identity. Everything else (e.g.
     * {@code payment-id-source}, {@code x-country-code}) is forwardable so a caller can drive
     * provider-supported request options per the provider's standard flow.
     */
    private static final java.util.Set<String> PROTECTED_HEADERS = java.util.Set.of(
            "authorization", "x-api-key", "x-jws-signature", "customer-profile-id",
            "sullis-api-key", "client-secret", "cookie");

    private boolean isForwardableHeader(String name) {
        return name != null && !PROTECTED_HEADERS.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Build headers for a {@code multipart/form-data} call. Identical to
     * {@link #buildHeaders} for auth + custom headers, but never forces
     * {@code application/json} — any Content-Type from config/caller is dropped so
     * the multipart converter can set {@code multipart/form-data} with its boundary.
     */
    private HttpHeaders buildMultipartHeaders(ThirdPartyProvider provider, ApiEnvironmentConfig envConfig,
                                              Map<String, String> additionalHeaders) {
        var httpHeaders = new HttpHeaders();

        if (envConfig != null && envConfig.getHeaders() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> configHeaders = objectMapper.readValue(envConfig.getHeaders(), Map.class);
                configHeaders.forEach((k, v) -> {
                    if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(k)) httpHeaders.set(k, v);
                });
            } catch (Exception e) {
                log.warn("Failed to parse env config headers: {}", e.getMessage());
            }
        }

        if (envConfig != null && envConfig.getCredentials() != null) {
            applyAuth(httpHeaders, envConfig.getAuthType() != null ? envConfig.getAuthType() : provider.getAuthType(),
                    envConfig.getCredentials());
        }

        if (additionalHeaders != null) {
            additionalHeaders.forEach((k, v) -> {
                if (!HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(k) && isForwardableHeader(k)) {
                    httpHeaders.set(k, v);
                }
            });
        }

        // Let the FormHttpMessageConverter append the boundary parameter.
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        return httpHeaders;
    }

    /**
     * Converts a JSON object body to {@code key=value&key2=value2} (URL-encoded) when the
     * resolved Content-Type is {@code application/x-www-form-urlencoded} — required by
     * providers such as Twilio. For any other content type the body is returned unchanged,
     * so existing JSON providers are not affected.
     */
    private String encodeBodyForContentType(String body, HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType == null
                || !MediaType.APPLICATION_FORM_URLENCODED.includes(contentType)
                || body == null || body.isBlank()) {
            return body;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(body, Map.class);
            StringBuilder sb = new StringBuilder();
            for (var entry : map.entrySet()) {
                if (entry.getValue() == null) continue;
                if (sb.length() > 0) sb.append('&');
                sb.append(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8))
                  .append('=')
                  .append(java.net.URLEncoder.encode(String.valueOf(entry.getValue()),
                          java.nio.charset.StandardCharsets.UTF_8));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to form-encode body, sending as-is: {}", e.getMessage());
            return body;
        }
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

    /**
     * Resolve per-request dynamic header tokens in-place. A header value containing a token
     * is rewritten before the call is made / persisted. Provider-agnostic — only headers that
     * carry a token are touched, so existing providers are unaffected.
     *
     * <ul>
     *   <li>{@code {{TRACE_ID}}} / {@code {{SPAN_ID}}} -> a fresh 16-hex B3 id</li>
     *   <li>{@code {{JWS}}} -> a detached RS256 JWS over the outgoing body, signed with
     *       {@code credentials.jwsPrivateKey} (PKCS#8 PEM). Empty if no key is configured.</li>
     * </ul>
     */
    private void resolveDynamicHeaders(HttpHeaders headers, String body, ApiEnvironmentConfig envConfig) {
        if (headers.isEmpty()) return;
        var updates = new LinkedHashMap<String, String>();
        headers.forEach((name, values) -> {
            if (values == null || values.isEmpty()) return;
            String value = values.get(0);
            if (value == null || !value.contains("{{")) return;
            String resolved = value
                    .replace("{{TRACE_ID}}", randomHex16())
                    .replace("{{SPAN_ID}}", randomHex16())
                    .replace("{{UUID}}", UUID.randomUUID().toString())
                    .replace("{{RANDOM_KEY}}", randomKey(32));
            if (resolved.contains("{{JWS}}")) {
                String signature = signJws(body, envConfig);
                resolved = resolved.replace("{{JWS}}", signature != null ? signature : "");
            }
            updates.put(name, resolved);
        });
        updates.forEach(headers::set);
    }

    private String signJws(String body, ApiEnvironmentConfig envConfig) {
        String pem = credentialValue(envConfig, "jwsPrivateKey");
        if (pem == null || pem.isBlank() || "TBD".equalsIgnoreCase(pem)) {
            log.warn("x-jws-signature requested but no jwsPrivateKey configured — sending empty signature");
            return null;
        }
        try {
            return JwsSignatureUtil.detachedRs256(body == null ? "" : body, pem);
        } catch (Exception e) {
            log.error("Failed to generate JWS signature: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String credentialValue(ApiEnvironmentConfig envConfig, String key) {
        if (envConfig == null || envConfig.getCredentials() == null || envConfig.getCredentials().isBlank()) {
            return null;
        }
        try {
            Map<String, String> creds = objectMapper.readValue(envConfig.getCredentials(), Map.class);
            return creds.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private String randomHex16() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }

    /** Random alphanumeric key of the given length (e.g. for an {@code x-api-key} placeholder). */
    private String randomKey(int length) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    /** True when the env config opts into local mock serving via credentials.mockMode=true. */
    private boolean isMockMode(ApiEnvironmentConfig envConfig) {
        return "true".equalsIgnoreCase(credentialValue(envConfig, "mockMode"));
    }

    /**
     * Expand the caller's simple flat body into the exact provider body using the API's
     * {@code request_template} (if any). Returns the caller body unchanged when no template is
     * configured. On a render error the caller body is sent as-is (fail-open) and a warning logged.
     */
    private String applyRequestTemplate(ProviderApi providerApi, String callerBody) {
        String template = providerApi.getRequestTemplate();
        if (template == null || template.isBlank()) return callerBody;
        try {
            var templateNode = objectMapper.readTree(template);
            var dataNode = (callerBody == null || callerBody.isBlank())
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(callerBody);
            var rendered = JsonTemplateRenderer.render(templateNode, dataNode, objectMapper);
            String full = objectMapper.writeValueAsString(rendered);
            log.debug("Expanded simple request via template for apiCode={}", providerApi.getCode());
            return full;
        } catch (Exception e) {
            log.warn("Request template render failed for {} — sending caller body as-is: {}",
                    providerApi.getCode(), e.getMessage());
            return callerBody;
        }
    }
}
