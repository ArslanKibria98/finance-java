package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Repayment Schedule Report — full installment plan for a loan")
public record RepaymentScheduleReportResponse(
    @Schema(description = "Loan ID") UUID loanId,
    @Schema(description = "Loan account number") String loanAccountNumber,
    @Schema(description = "Customer name") String customerName,
    @Schema(description = "Product name") String productName,
    @Schema(description = "Disbursed principal") BigDecimal disbursedPrincipal,
    @Schema(description = "Total profit") BigDecimal totalProfit,
    @Schema(description = "Total payable") BigDecimal totalPayable,
    @Schema(description = "Tenure in months") Integer tenureMonths,
    @Schema(description = "Installment line items") List<Installment> installments
) {

    @Builder
    @Schema(description = "Per-installment schedule line")
    public record Installment(
        @Schema(description = "Installment number") Integer installmentNumber,
        @Schema(description = "Principal due") BigDecimal principalDue,
        @Schema(description = "Profit / interest due") BigDecimal profitDue,
        @Schema(description = "Total installment amount") BigDecimal installmentAmount,
        @Schema(description = "Remaining principal after this installment") BigDecimal remainingPrincipal,
        @Schema(description = "Penalty accrued on this installment") BigDecimal penaltyAmount,
        @Schema(description = "Due date") LocalDate dueDate,
        @Schema(description = "Status (PENDING / PAID / PARTIALLY_PAID / OVERDUE)") String status,
        @Schema(description = "Payment date (null if unpaid)") LocalDate paymentDate,
        @Schema(description = "Amount paid so far") BigDecimal amountPaid
    ) {}
}
