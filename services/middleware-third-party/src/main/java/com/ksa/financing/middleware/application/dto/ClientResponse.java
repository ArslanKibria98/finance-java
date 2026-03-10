package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String code,
        String description,
        String secretKey,
        String callbackUrl,
        List<String> ipWhitelist,
        String status,
        String environment,
        Instant createdAt,
        Instant updatedAt
) {}
