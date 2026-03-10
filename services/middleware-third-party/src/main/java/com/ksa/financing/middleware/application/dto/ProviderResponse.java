package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        String code,
        String name,
        String description,
        String category,
        String baseUrlDev,
        String baseUrlProd,
        String authType,
        String status,
        int timeoutMs,
        int retryCount,
        Instant createdAt,
        Instant updatedAt
) {}
