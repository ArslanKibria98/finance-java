package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Loan Disbursement Report — list of loans disbursed within date range")
public record LoanDisbursementReportResponse(
    @Schema(description = "Report from date") LocalDate fromDate,
    @Schema(description = "Report to date") LocalDate toDate,
    @Schema(description = "Number of disbursed loans") int totalCount,
    @Schema(description = "Total disbursed amount (SAR)") BigDecimal totalDisbursedAmount,
    @Schema(description = "Line items") List<Line> items
) {

    @Builder
    @Schema(description = "Per-loan disbursement line")
    public record Line(
        @Schema(description = "Application number") String applicationNumber,
        @Schema(description = "Loan account number") String loanAccountNumber,
        @Schema(description = "Customer ID") UUID customerId,
        @Schema(description = "Customer full name") String customerName,
        @Schema(description = "National / Iqama ID") String nationalId,
        @Schema(description = "Product code") String productCode,
        @Schema(description = "Product name") String productName,
        @Schema(description = "Disbursement date") LocalDate disbursementDate,
        @Schema(description = "Disbursed amount") BigDecimal disbursedAmount,
        @Schema(description = "Tenure in months") Integer tenureMonths,
        @Schema(description = "Current loan status") String status,
        @Schema(description = "Branch / channel") String branchOrChannel
    ) {}
}
