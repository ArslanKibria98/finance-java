package com.ksa.financing.ledger.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductCoaFieldResponse(
        UUID id,
        UUID productId,
        UUID coaFieldId,
        String fieldCode,
        String fieldName,
        String fieldNameAr,
        String fieldType,
        boolean isMandatory,
        UUID assignedAccountId,
        String assignedAccountCode,
        String assignedAccountName,
        String status,
        LocalDateTime assignedAt
) {}
