package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "Trial Balance Report - All GL accounts and their balances")
public record TrialBalanceReportResponse(
        @Schema(description = "Date of the report")
        LocalDate reportDate,

        @Schema(description = "List of all accounts with balances")
        List<TrialBalanceAccountResponse> accounts,

        @Schema(description = "Total debits across all accounts")
        BigDecimal totalDebits,

        @Schema(description = "Total credits across all accounts")
        BigDecimal totalCredits,

        @Schema(description = "Difference (should be 0 for balanced ledger)")
        BigDecimal difference
) {}
