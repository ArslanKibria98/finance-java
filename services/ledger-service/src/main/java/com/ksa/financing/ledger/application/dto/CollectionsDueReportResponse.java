package com.ksa.financing.ledger.application.dto;

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
        List<CollectionsDueItem> items
) {
    @Builder
    public record CollectionsDueItem(
            UUID loanId,
            UUID customerId,
            LocalDate dueDate,
            BigDecimal dueAmount
    ) {}
}
