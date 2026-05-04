package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

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
