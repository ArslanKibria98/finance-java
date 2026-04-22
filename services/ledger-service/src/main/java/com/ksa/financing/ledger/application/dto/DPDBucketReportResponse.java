package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "DPD Bucket Report - Delinquency Analysis")
public record DPDBucketReportResponse(
    @Schema(description = "Report date")
    LocalDate reportDate,
    @Schema(description = "List of DPD buckets")
    List<DPDBucketDto> buckets
) {}

record DPDBucketDto(
    @Schema(description = "DPD range (e.g., '0-30')")
    String dpdRange,
    @Schema(description = "Number of loans in this bucket")
    Integer loanCount,
    @Schema(description = "Total outstanding amount in this bucket")
    BigDecimal totalAmount
) {}
