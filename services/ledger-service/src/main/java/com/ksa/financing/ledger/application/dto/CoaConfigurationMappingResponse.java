package com.ksa.financing.ledger.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoaConfigurationMappingResponse(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID profileId,
        UUID coaFieldId,
        UUID accountId,
        Boolean mandatoryOverride,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
