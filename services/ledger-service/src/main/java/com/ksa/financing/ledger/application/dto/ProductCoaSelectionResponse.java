package com.ksa.financing.ledger.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductCoaSelectionResponse(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID coaFieldId,
        String fieldKey,
        String fieldLabelEn,
        UUID accountId,
        String accountCode,
        String accountName,
        Boolean mandatoryOverride,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
