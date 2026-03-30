package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ProviderApiResponse(
        UUID id,
        UUID providerId,
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        String httpMethod,
        String endpointPath,
        String status,
        boolean async,
        Integer timeoutMs,
        Instant createdAt,
        Instant updatedAt
) {}
