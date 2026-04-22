package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
@Schema(description = "Write-off & Provisions Report")
public record WriteOffProvisionReportResponse(
    @Schema(description = "Period (YYYY-MM)")
    String period,
    @Schema(description = "Total write-offs")
    BigDecimal totalWriteOffs,
    @Schema(description = "Bad debt provisions")
    BigDecimal badDebtProvisions,
    @Schema(description = "Restructured loans")
    BigDecimal restructuredLoans
) {}
