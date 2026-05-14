package com.ksa.financing.middleware.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for a third-party API call routed through the middleware.
 *
 * Persisted into one of three environment-specific tables:
 *   TEST -> client_request_test  (mock dispatcher response)
 *   DEV  -> client_request_dev   (live call against provider DEV credentials)
 *   PROD -> client_request_prod  (live call against provider PROD credentials)
 *
 * Each row captures the full request/response envelope plus business keys
 * (national_id, mobile_number, idempotency_key) so the operations console can
 * filter and order audit entries per environment.
 */
public class ClientRequest {

    private UUID id;
    private UUID tenantId;
    private UUID apiId;
    private UUID clientId;
    private String requestId;
    private String providerCode;
    private String apiCode;
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
    private String mobileNumber;
    private String callerService;
    private UUID customerId;
    private String applicationId;
    private String contextType;
    private BigDecimal apiCost;
    private String costCurrency;
    private Instant createdAt;

    public ClientRequest() {}

    public static ClientRequest create(UUID tenantId, UUID apiId, UUID clientId, String requestId,
                                       String providerCode, String apiCode,
                                       EnvironmentType environment, HttpMethod httpMethod,
                                       String requestUrl, String requestHeaders, String requestBody,
                                       String idempotencyKey, String nationalId, String mobileNumber,
                                       String callerService) {
        return create(tenantId, apiId, clientId, requestId, providerCode, apiCode, environment,
                httpMethod, requestUrl, requestHeaders, requestBody, idempotencyKey, nationalId,
                mobileNumber, callerService, null, null, null, null, null);
    }

    public static ClientRequest create(UUID tenantId, UUID apiId, UUID clientId, String requestId,
                                       String providerCode, String apiCode,
                                       EnvironmentType environment, HttpMethod httpMethod,
                                       String requestUrl, String requestHeaders, String requestBody,
                                       String idempotencyKey, String nationalId, String mobileNumber,
                                       String callerService,
                                       UUID customerId, String applicationId, String contextType,
                                       BigDecimal apiCost, String costCurrency) {
        var req = new ClientRequest();
        req.tenantId = tenantId;
        req.apiId = apiId;
        req.clientId = clientId;
        req.requestId = requestId;
        req.providerCode = providerCode;
        req.apiCode = apiCode;
        req.environment = environment;
        req.httpMethod = httpMethod;
        req.requestUrl = requestUrl;
        req.requestHeaders = requestHeaders;
        req.requestBody = requestBody;
        req.status = RequestStatus.PENDING;
        req.idempotencyKey = idempotencyKey;
        req.nationalId = nationalId;
        req.mobileNumber = mobileNumber;
        req.callerService = callerService;
        req.customerId = customerId;
        req.applicationId = applicationId;
        req.contextType = contextType;
        req.apiCost = apiCost;
        req.costCurrency = costCurrency;
        return req;
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

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getApiId() { return apiId; }
    public UUID getClientId() { return clientId; }
    public String getRequestId() { return requestId; }
    public String getProviderCode() { return providerCode; }
    public String getApiCode() { return apiCode; }
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
    public String getMobileNumber() { return mobileNumber; }
    public String getCallerService() { return callerService; }
    public UUID getCustomerId() { return customerId; }
    public String getApplicationId() { return applicationId; }
    public String getContextType() { return contextType; }
    public BigDecimal getApiCost() { return apiCost; }
    public String getCostCurrency() { return costCurrency; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setApiId(UUID apiId) { this.apiId = apiId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public void setApiCode(String apiCode) { this.apiCode = apiCode; }
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
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public void setCallerService(String callerService) { this.callerService = callerService; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public void setContextType(String contextType) { this.contextType = contextType; }
    public void setApiCost(BigDecimal apiCost) { this.apiCost = apiCost; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
