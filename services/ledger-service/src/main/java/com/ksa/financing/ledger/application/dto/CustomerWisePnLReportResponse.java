package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record CustomerWisePnLReportResponse(
        String period,
        List<CustomerWisePnLItem> items,
        @JsonIgnore PageMetadata pagination
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
