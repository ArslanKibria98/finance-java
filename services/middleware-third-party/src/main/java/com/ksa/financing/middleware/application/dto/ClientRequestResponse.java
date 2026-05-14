package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientRequestResponse(
        UUID id,
        UUID tenantId,
        UUID apiId,
        UUID clientId,
        String clientName,
        String requestId,
        String providerCode,
        String providerName,
        String apiCode,
        String apiName,
        String serviceName,
        String environment,
        String httpMethod,
        String requestUrl,
        Object requestHeaders,
        Object requestBody,
        Integer responseStatus,
        Object responseHeaders,
        Object responseBody,
        String status,
        Long durationMs,
        String errorMessage,
        String idempotencyKey,
        String nationalId,
        String mobileNumber,
        String callerService,
        Instant createdAt
) {}
