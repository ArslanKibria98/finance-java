package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Customer Statement of Account — chronological ledger for a single customer")
public record CustomerStatementOfAccountResponse(
    @Schema(description = "Customer ID") UUID customerId,
    @Schema(description = "Customer name") String customerName,
    @Schema(description = "National / Iqama ID") String nationalId,
    @Schema(description = "Statement from date") LocalDate fromDate,
    @Schema(description = "Statement to date") LocalDate toDate,
    @Schema(description = "Opening balance") BigDecimal openingBalance,
    @Schema(description = "Closing balance") BigDecimal closingBalance,
    @Schema(description = "Total debits") BigDecimal totalDebits,
    @Schema(description = "Total credits") BigDecimal totalCredits,
    @Schema(description = "Transaction entries") List<Entry> entries,
    @Schema(description = "Pagination metadata") @JsonIgnore PageMetadata pagination
) {

    @Builder
    @Schema(description = "Ledger entry line")
    public record Entry(
        @Schema(description = "Transaction date") LocalDate transactionDate,
        @Schema(description = "Transaction type (DISBURSEMENT / REPAYMENT / FEE / PENALTY / WRITE_OFF / ADJUSTMENT)") String transactionType,
        @Schema(description = "Description") String description,
        @Schema(description = "Reference (loan account / payment ref)") String reference,
        @Schema(description = "Debit amount") BigDecimal debit,
        @Schema(description = "Credit amount") BigDecimal credit,
        @Schema(description = "Running balance") BigDecimal runningBalance
    ) {}
}
