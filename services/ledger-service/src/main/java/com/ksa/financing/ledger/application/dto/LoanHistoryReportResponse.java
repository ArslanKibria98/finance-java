package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record LoanHistoryReportResponse(
        UUID loanId,
        List<LoanHistoryItem> events
) {
    @Builder
    public record LoanHistoryItem(
            LocalDateTime eventTime,
            String eventType,
            String description
    ) {}
}
