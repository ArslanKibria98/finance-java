package com.ksa.financing.ledger.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ksa.financing.infra.pagination.PageMetadata;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ledger Report — per-account general ledger view with opening balance,
 * chronological movements, and closing balance. One account per response
 * (or multiple when accountCode is not provided).
 */
@Builder
public record LedgerReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        int totalAccounts,
        List<AccountLedger> accounts,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record AccountLedger(
            UUID accountId,
            String accountCode,
            String accountName,
            String accountType,
            String currency,
            BigDecimal openingBalance,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            BigDecimal closingBalance,
            List<LedgerMovement> movements
    ) {}

    @Builder
    public record LedgerMovement(
            LocalDate entryDate,
            UUID entryId,
            String voucherNumber,
            String referenceType,
            UUID referenceId,
            String transactionType,
            String description,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal runningBalance,
            String status
    ) {}
}
