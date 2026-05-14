package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record AccountReportResponse(
        LocalDate asOfDate,
        List<AccountReportItem> items,
        @JsonIgnore PageMetadata pagination
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
