package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Overdue Loan Report — loans with past-due installments")
public record OverdueLoanReportResponse(
    @Schema(description = "As-of date") LocalDate asOfDate,
    @Schema(description = "Number of overdue loans") int totalCount,
    @Schema(description = "Total overdue amount (SAR)") BigDecimal totalOverdueAmount,
    @Schema(description = "Line items") List<Line> items
) {

    @Builder
    @Schema(description = "Per-loan overdue line")
    public record Line(
        @Schema(description = "Loan account number") String loanAccountNumber,
        @Schema(description = "Customer ID") UUID customerId,
        @Schema(description = "Customer name") String customerName,
        @Schema(description = "National / Iqama ID") String nationalId,
        @Schema(description = "Product name") String productName,
        @Schema(description = "Principal overdue") BigDecimal principalOverdue,
        @Schema(description = "Profit overdue") BigDecimal profitOverdue,
        @Schema(description = "Penalty accrued") BigDecimal penaltyAmount,
        @Schema(description = "Total overdue") BigDecimal totalOverdue,
        @Schema(description = "Days past due") Integer daysPastDue,
        @Schema(description = "DPD bucket (1-30 / 31-60 / 61-90 / 90+)") String dpdBucket,
        @Schema(description = "Earliest unpaid installment date") LocalDate oldestUnpaidDate,
        @Schema(description = "Loan status") String status
    ) {}
}
