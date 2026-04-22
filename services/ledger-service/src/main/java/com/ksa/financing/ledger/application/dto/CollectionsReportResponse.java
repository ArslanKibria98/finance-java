package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(description = "Collections & Repayment Report")
public record CollectionsReportResponse(
    @Schema(description = "From date")
    LocalDate fromDate,
    @Schema(description = "To date")
    LocalDate toDate,
    @Schema(description = "Total amount collected")
    BigDecimal totalCollected,
    @Schema(description = "On-time collection rate")
    BigDecimal onTimeRate,
    @Schema(description = "Number of collections processed")
    Integer collectionCount
) {}
