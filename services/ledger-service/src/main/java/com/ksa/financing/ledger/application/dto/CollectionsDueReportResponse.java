package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record CollectionsDueReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalDue,
        List<CollectionsDueItem> items,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record CollectionsDueItem(
            UUID loanId,
            UUID customerId,
            LocalDate dueDate,
            BigDecimal dueAmount
    ) {}
}
