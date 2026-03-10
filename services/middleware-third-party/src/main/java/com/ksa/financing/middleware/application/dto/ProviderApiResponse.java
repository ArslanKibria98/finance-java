package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ProviderApiResponse(
        UUID id,
        UUID providerId,
        String code,
        String name,
        String description,
        String httpMethod,
        String endpointPath,
        String status,
        boolean async,
        Integer timeoutMs,
        Instant createdAt,
        Instant updatedAt
) {}
