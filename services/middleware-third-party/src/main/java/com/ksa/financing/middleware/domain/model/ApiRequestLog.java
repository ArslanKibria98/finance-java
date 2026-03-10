package com.ksa.financing.middleware.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ApiRequestLog {
    private UUID id;
    private UUID tenantId;
    private UUID apiId;
    private UUID clientId;
    private String requestId;
    private EnvironmentType environment;
    private HttpMethod httpMethod;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseHeaders;
    private String responseBody;
    private RequestStatus status;
    private Long durationMs;
    private String errorMessage;
    private String idempotencyKey;
    private String nationalId;
    private Instant createdAt;

    public ApiRequestLog() {}

    public static ApiRequestLog create(UUID tenantId, UUID apiId, UUID clientId, String requestId,
                                        EnvironmentType environment, HttpMethod httpMethod,
                                        String requestUrl, String requestHeaders, String requestBody,
                                        String idempotencyKey, String nationalId) {
        var log = new ApiRequestLog();
        log.tenantId = tenantId;
        log.apiId = apiId;
        log.clientId = clientId;
        log.requestId = requestId;
        log.environment = environment;
        log.httpMethod = httpMethod;
        log.requestUrl = requestUrl;
        log.requestHeaders = requestHeaders;
        log.requestBody = requestBody;
        log.status = RequestStatus.PENDING;
        log.idempotencyKey = idempotencyKey;
        log.nationalId = nationalId;
        return log;
    }

    public void markSuccess(int responseStatus, String responseHeaders, String responseBody, long durationMs) {
        this.responseStatus = responseStatus;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.durationMs = durationMs;
        this.status = RequestStatus.SUCCESS;
    }

    public void markFailed(int responseStatus, String responseBody, String errorMessage, long durationMs) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.status = RequestStatus.FAILED;
    }

    public void markTimeout(String errorMessage, long durationMs) {
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.status = RequestStatus.TIMEOUT;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getApiId() { return apiId; }
    public UUID getClientId() { return clientId; }
    public String getRequestId() { return requestId; }
    public EnvironmentType getEnvironment() { return environment; }
    public HttpMethod getHttpMethod() { return httpMethod; }
    public String getRequestUrl() { return requestUrl; }
    public String getRequestHeaders() { return requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public Integer getResponseStatus() { return responseStatus; }
    public String getResponseHeaders() { return responseHeaders; }
    public String getResponseBody() { return responseBody; }
    public RequestStatus getStatus() { return status; }
    public Long getDurationMs() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getNationalId() { return nationalId; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setApiId(UUID apiId) { this.apiId = apiId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setEnvironment(EnvironmentType environment) { this.environment = environment; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
