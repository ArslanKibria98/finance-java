package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record EarlySettlementReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        List<EarlySettlementItem> items
) {
    @Builder
    public record EarlySettlementItem(
            UUID loanId,
            UUID customerId,
            LocalDate settlementDate,
            BigDecimal outstandingAmount,
            BigDecimal rebateAmount
    ) {}
}
