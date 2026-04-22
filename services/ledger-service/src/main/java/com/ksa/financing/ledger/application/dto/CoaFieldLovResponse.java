package com.ksa.financing.ledger.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoaFieldLovResponse(
        UUID id,
        UUID tenantId,
        String fieldKey,
        String fieldLabelEn,
        String fieldLabelAr,
        String category,
        boolean mandatoryDefault,
        int displayOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
