package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record AccountReportResponse(
        LocalDate asOfDate,
        List<AccountReportItem> items
) {
    @Builder
    public record AccountReportItem(
            String accountCode,
            String accountName,
            BigDecimal openingBalance,
            BigDecimal debits,
            BigDecimal credits,
            BigDecimal closingBalance
    ) {}
}
