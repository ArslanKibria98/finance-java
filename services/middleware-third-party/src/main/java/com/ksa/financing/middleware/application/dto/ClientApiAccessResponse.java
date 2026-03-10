package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientApiAccessResponse(
        UUID id,
        UUID clientId,
        UUID apiId,
        String apiName,
        String apiCode,
        UUID providerId,
        String providerName,
        String environment,
        boolean active,
        Instant grantedAt
) {}
