package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record DailyTransactionSummaryReportResponse(
        LocalDate date,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        Integer transactionCount,
        List<ChannelSummary> channels
) {
    @Builder
    public record ChannelSummary(
            String channel,
            BigDecimal amount,
            Integer count
    ) {}
}
