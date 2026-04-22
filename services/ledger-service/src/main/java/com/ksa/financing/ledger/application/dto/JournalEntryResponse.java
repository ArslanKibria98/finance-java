package com.ksa.financing.ledger.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for journal entry queries.
 */
public record JournalEntryResponse(
        UUID id,
        UUID tenantId,
        String entryNumber,
        String referenceType,
        UUID referenceId,
        String transactionType,
        LocalDate entryDate,
        LocalDate valueDate,
        String currency,
        String description,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        String status,
        boolean isReversal,
        UUID originalEntryId,
        boolean fineractSynced,
        Long fineractTransactionId,
        List<JournalLineResponse> lines,
        UUID createdBy,
        LocalDateTime createdAt
) {

    public record JournalLineResponse(
            String accountCode,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String description,
            int lineNumber
    ) {}
}
