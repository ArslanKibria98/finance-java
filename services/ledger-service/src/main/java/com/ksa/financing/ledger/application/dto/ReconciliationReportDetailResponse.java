package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDate;

@Builder
@Schema(description = "Reconciliation Report - GL vs Fineract")
public record ReconciliationReportDetailResponse(
    @Schema(description = "Report date")
    LocalDate reportDate,
    @Schema(description = "Total entries in our GL")
    Integer ourGlCount,
    @Schema(description = "Total entries in Fineract GL")
    Integer fineractGlCount,
    @Schema(description = "Matching entries")
    Integer matchingEntries,
    @Schema(description = "Discrepancies found")
    Integer discrepancies
) {}
