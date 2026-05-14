package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record LoanHistoryReportResponse(
        UUID loanId,
        List<LoanHistoryItem> events,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record LoanHistoryItem(
            LocalDateTime eventTime,
            String eventType,
            String description
    ) {}
}
