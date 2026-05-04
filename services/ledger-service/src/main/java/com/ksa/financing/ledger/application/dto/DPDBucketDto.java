package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "DPD Bucket entry")
public record DPDBucketDto(
        @Schema(description = "DPD range (e.g., '0-30')")
        String dpdRange,
        @Schema(description = "Number of loans in this bucket")
        Integer loanCount,
        @Schema(description = "Total outstanding amount in this bucket")
        BigDecimal totalAmount
) {}
