package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(description = "Portfolio Summary Report")
public record PortfolioSummaryReportResponse(
    @Schema(description = "Report from date")
    LocalDate fromDate,
    @Schema(description = "Report to date")
    LocalDate toDate,
    @Schema(description = "Total amount disbursed")
    BigDecimal totalDisbursed,
    @Schema(description = "Outstanding balance")
    BigDecimal outstandingBalance,
    @Schema(description = "Total collections")
    BigDecimal totalCollections,
    @Schema(description = "Delinquency rate percentage")
    BigDecimal delinquencyRate
) {}
