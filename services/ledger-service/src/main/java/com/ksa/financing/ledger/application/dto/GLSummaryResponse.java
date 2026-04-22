package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
@Schema(description = "GL Summary Response")
public record GLSummaryResponse(
    @Schema(description = "Total debits")
    BigDecimal totalDebits,
    @Schema(description = "Total credits")
    BigDecimal totalCredits,
    @Schema(description = "Balance difference")
    BigDecimal difference
) {}
