package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EnvConfigResponse(
        UUID id,
        UUID apiId,
        String environment,
        String baseUrl,
        String endpointPath,
        String credentials,
        String headers,
        String queryParams,
        String authType,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
