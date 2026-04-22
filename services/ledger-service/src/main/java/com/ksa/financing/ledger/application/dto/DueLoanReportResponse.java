package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Due Loan Report — upcoming installments in the selected window")
public record DueLoanReportResponse(
    @Schema(description = "Window from date") LocalDate fromDate,
    @Schema(description = "Window to date") LocalDate toDate,
    @Schema(description = "Number of loans with due installments") int totalCount,
    @Schema(description = "Total amount due in window (SAR)") BigDecimal totalDueAmount,
    @Schema(description = "Line items") List<Line> items
) {

    @Builder
    @Schema(description = "Per-installment due line")
    public record Line(
        @Schema(description = "Loan account number") String loanAccountNumber,
        @Schema(description = "Customer ID") UUID customerId,
        @Schema(description = "Customer name") String customerName,
        @Schema(description = "Product name") String productName,
        @Schema(description = "Installment number") Integer installmentNumber,
        @Schema(description = "Due date") LocalDate dueDate,
        @Schema(description = "Principal due") BigDecimal principalDue,
        @Schema(description = "Profit due") BigDecimal profitDue,
        @Schema(description = "Total installment amount") BigDecimal installmentAmount,
        @Schema(description = "Days until due") Integer daysUntilDue,
        @Schema(description = "Installment status") String status
    ) {}
}
