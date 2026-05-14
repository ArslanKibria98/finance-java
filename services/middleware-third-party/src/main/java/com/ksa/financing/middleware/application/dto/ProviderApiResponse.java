package com.ksa.financing.middleware.application.dto;

import java.math.BigDecimal;
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
        BigDecimal costPerCall,
        String costCurrency,
        Instant createdAt,
        Instant updatedAt
) {}
