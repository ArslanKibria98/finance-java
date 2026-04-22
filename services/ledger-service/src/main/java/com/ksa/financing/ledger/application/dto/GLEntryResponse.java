package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(description = "GL Entry Response")
public record GLEntryResponse(
    @Schema(description = "Entry ID")
    String entryId,
    @Schema(description = "Entry date")
    LocalDate entryDate,
    @Schema(description = "Account code")
    String accountCode,
    @Schema(description = "Debit amount")
    BigDecimal debitAmount,
    @Schema(description = "Credit amount")
    BigDecimal creditAmount
) {}
