package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CallbackResponseDto(
        UUID id,
        UUID apiId,
        UUID clientId,
        UUID requestLogId,
        String callbackData,
        String status,
        Instant processedAt,
        String errorMessage,
        Instant createdAt
) {}
