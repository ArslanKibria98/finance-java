package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Schema(description = "Product-wise P&L report")
public record ProductWisePnLReportResponse(
        @Schema(description = "Period in YYYY-MM format")
        String period,
        @Schema(description = "P&L rows grouped by product")
        List<ProductWisePnLItem> items,
        @Schema(description = "Pagination metadata") @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record ProductWisePnLItem(
            String productCode,
            String productName,
            BigDecimal revenue,
            BigDecimal expense,
            BigDecimal netProfit
    ) {}
}
