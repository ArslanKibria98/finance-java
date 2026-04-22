package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "GL Reconciliation Response")
public record GLReconciliationResponse(
    @Schema(description = "Status")
    String status
) {}
