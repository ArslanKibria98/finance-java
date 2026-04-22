package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.ledger.domain.model.AccountType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for GL account queries.
 */
public record AccountResponse(
        UUID id,
        UUID tenantId,
        String accountCode,
        String accountName,
        String accountNameAr,
        AccountType accountType,
        UUID parentAccountId,
        int hierarchyLevel,
        boolean isHeader,
        boolean isManualEntriesAllowed,
        String status,
        String iban,
        UUID fineractMappingId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
