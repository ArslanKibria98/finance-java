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
@Schema(description = "Loan Balance & Outstanding Report — live balances across the portfolio")
public record LoanBalanceOutstandingReportResponse(
    @Schema(description = "As-of date") LocalDate asOfDate,
    @Schema(description = "Number of loans") int totalCount,
    @Schema(description = "Total principal outstanding") BigDecimal totalPrincipalOutstanding,
    @Schema(description = "Total profit outstanding") BigDecimal totalProfitOutstanding,
    @Schema(description = "Total penalties outstanding") BigDecimal totalPenaltiesOutstanding,
    @Schema(description = "Line items") List<Line> items,
    @Schema(description = "Pagination metadata") @JsonIgnore PageMetadata pagination
) {

    @Builder
    @Schema(description = "Per-loan balance line")
    public record Line(
        @Schema(description = "Loan account number") String loanAccountNumber,
        @Schema(description = "Customer ID") UUID customerId,
        @Schema(description = "Customer name") String customerName,
        @Schema(description = "Product name") String productName,
        @Schema(description = "Disbursed amount") BigDecimal disbursedAmount,
        @Schema(description = "Total paid to date") BigDecimal totalPaid,
        @Schema(description = "Principal outstanding") BigDecimal principalOutstanding,
        @Schema(description = "Profit outstanding") BigDecimal profitOutstanding,
        @Schema(description = "Penalties outstanding") BigDecimal penaltiesOutstanding,
        @Schema(description = "Next due date") LocalDate nextDueDate,
        @Schema(description = "Next due amount") BigDecimal nextDueAmount,
        @Schema(description = "Loan status") String loanStatus
    ) {}
}
