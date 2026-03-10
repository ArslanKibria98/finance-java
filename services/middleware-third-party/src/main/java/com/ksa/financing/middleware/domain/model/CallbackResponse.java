package com.ksa.financing.middleware.domain.model;

import java.time.Instant;
import java.util.UUID;

public class CallbackResponse {
    private UUID id;
    private UUID tenantId;
    private UUID apiId;
    private UUID clientId;
    private UUID requestLogId;
    private String callbackData;
    private CallbackStatus status;
    private Instant processedAt;
    private String errorMessage;
    private Instant createdAt;

    public CallbackResponse() {}

    public static CallbackResponse create(UUID tenantId, UUID apiId, UUID clientId,
                                           UUID requestLogId, String callbackData) {
        var callback = new CallbackResponse();
        callback.tenantId = tenantId;
        callback.apiId = apiId;
        callback.clientId = clientId;
        callback.requestLogId = requestLogId;
        callback.callbackData = callbackData;
        callback.status = CallbackStatus.RECEIVED;
        return callback;
    }

    public void markProcessed() {
        this.status = CallbackStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = CallbackStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getApiId() { return apiId; }
    public UUID getClientId() { return clientId; }
    public UUID getRequestLogId() { return requestLogId; }
    public String getCallbackData() { return callbackData; }
    public CallbackStatus getStatus() { return status; }
    public Instant getProcessedAt() { return processedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters for mapper
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setApiId(UUID apiId) { this.apiId = apiId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public void setRequestLogId(UUID requestLogId) { this.requestLogId = requestLogId; }
    public void setCallbackData(String callbackData) { this.callbackData = callbackData; }
    public void setStatus(CallbackStatus status) { this.status = status; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
