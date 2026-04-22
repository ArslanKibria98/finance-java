package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
@Schema(description = "DPD Trend Response")
public record DPDTrendResponse(
    @Schema(description = "Period")
    String period,
    @Schema(description = "Trend value")
    BigDecimal value
) {}
