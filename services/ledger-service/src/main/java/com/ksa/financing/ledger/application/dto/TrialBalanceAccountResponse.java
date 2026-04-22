package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Account balance in trial balance report")
public record TrialBalanceAccountResponse(
        @Schema(description = "Account code")
        String accountCode,

        @Schema(description = "Account name")
        String accountName,

        @Schema(description = "Account type (ASSET, LIABILITY, etc)")
        String accountType,

        @Schema(description = "Debit balance")
        BigDecimal debitBalance,

        @Schema(description = "Credit balance")
        BigDecimal creditBalance
) {}
