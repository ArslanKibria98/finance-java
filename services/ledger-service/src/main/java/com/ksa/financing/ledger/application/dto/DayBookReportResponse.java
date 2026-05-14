package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Day Book Report — chronological listing of every debit and credit line
 * posted on a given date or range, one row per journal line.
 */
@Builder
public record DayBookReportResponse(
        LocalDate reportDate,
        int totalTransactions,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal difference,
        List<DayBookEntry> entries,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record DayBookEntry(
            LocalDateTime postedAt,
            UUID entryId,
            String voucherNumber,
            String referenceType,
            UUID referenceId,
            String transactionType,
            String description,
            int lineNumber,
            String accountCode,
            String accountName,
            String lineDescription,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            String status
    ) {}
}
