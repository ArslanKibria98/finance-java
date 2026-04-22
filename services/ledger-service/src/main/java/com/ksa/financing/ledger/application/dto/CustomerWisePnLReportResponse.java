package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record CustomerWisePnLReportResponse(
        String period,
        List<CustomerWisePnLItem> items
) {
    @Builder
    public record CustomerWisePnLItem(
            UUID customerId,
            String customerName,
            BigDecimal revenue,
            BigDecimal expense,
            BigDecimal netProfit
    ) {}
}
