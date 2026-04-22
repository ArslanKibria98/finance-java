package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(description = "Cash Flow Report - Bank Account Liquidity")
public record CashFlowReportResponse(
    @Schema(description = "Report date")
    LocalDate reportDate,
    @Schema(description = "Total inflows")
    BigDecimal totalInflows,
    @Schema(description = "Total outflows")
    BigDecimal totalOutflows,
    @Schema(description = "Net cash position")
    BigDecimal netPosition,
    @Schema(description = "Current bank balance")
    BigDecimal bankBalance
) {}
