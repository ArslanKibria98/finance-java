package com.ksa.financing.ledger.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CoaConfigurationProfileResponse(
        UUID id,
        UUID tenantId,
        String productCode,
        String profileName,
        String status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
