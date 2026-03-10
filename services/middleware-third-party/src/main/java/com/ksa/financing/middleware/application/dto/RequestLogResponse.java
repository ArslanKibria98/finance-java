package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RequestLogResponse(
        UUID id,
        UUID apiId,
        UUID clientId,
        String requestId,
        String environment,
        String httpMethod,
        String requestUrl,
        Integer responseStatus,
        String status,
        Long durationMs,
        String errorMessage,
        String idempotencyKey,
        Instant createdAt
) {}
