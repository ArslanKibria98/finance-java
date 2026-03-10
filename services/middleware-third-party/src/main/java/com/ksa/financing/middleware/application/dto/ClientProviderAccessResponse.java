package com.ksa.financing.middleware.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientProviderAccessResponse(
        UUID id,
        UUID clientId,
        UUID providerId,
        String providerName,
        String providerCode,
        String environment,
        boolean active,
        Instant grantedAt
) {}
