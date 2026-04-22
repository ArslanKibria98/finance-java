package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
@Schema(description = "Profit & Revenue Report")
public record ProfitRevenueReportResponse(
    @Schema(description = "Period (YYYY-MM)")
    String period,
    @Schema(description = "Profit earned")
    BigDecimal profitEarned,
    @Schema(description = "Profit collected")
    BigDecimal profitCollected,
    @Schema(description = "Accrued profit")
    BigDecimal accruedProfit
) {}
