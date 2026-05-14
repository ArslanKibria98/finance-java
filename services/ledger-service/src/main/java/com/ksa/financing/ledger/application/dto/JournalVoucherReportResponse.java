package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Journal Vouchers Report — lists all journal entries (vouchers) in a date range
 * with their debit / credit lines, grouped by voucher.
 */
@Builder
public record JournalVoucherReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        int totalVouchers,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        List<VoucherItem> vouchers,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record VoucherItem(
            UUID entryId,
            String voucherNumber,
            LocalDate entryDate,
            LocalDate valueDate,
            String referenceType,
            UUID referenceId,
            String transactionType,
            String description,
            String status,
            String currency,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            boolean isReversal,
            boolean fineractSynced,
            List<VoucherLine> lines
    ) {}

    @Builder
    public record VoucherLine(
            int lineNumber,
            String accountCode,
            String accountName,
            String description,
            BigDecimal debitAmount,
            BigDecimal creditAmount
    ) {}
}
