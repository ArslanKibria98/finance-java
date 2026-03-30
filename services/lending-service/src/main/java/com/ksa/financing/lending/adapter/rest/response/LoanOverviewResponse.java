package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * BRD UC#04 — Finance Overview response.
 * Shows active/completed loans with donut chart data and key details.
 */
@Schema(description = "BRD UC#04 — Finance Overview with loan breakdown")
public record LoanOverviewResponse(

        @Schema(description = "Active loans list")
        List<LoanSummary> activeLoans,

        @Schema(description = "Completed loans list")
        List<LoanSummary> completedLoans,

        @Schema(description = "Total paid amount across all loans in SAR")
        BigDecimal totalPaid,

        @Schema(description = "Total pending amount in SAR")
        BigDecimal totalPending,

        @Schema(description = "Total overdue amount in SAR")
        BigDecimal totalOverdue
) {

    @Schema(description = "Individual loan summary for overview")
    public record LoanSummary(

            @Schema(description = "Loan UUID")
            String loanId,

            @Schema(description = "Human-readable loan number")
            String loanNumber,

            @Schema(description = "Loan type (e.g., Personal Finance)")
            String loanType,

            @Schema(description = "Application ID reference")
            String applicationId,

            @Schema(description = "Finance amount in SAR")
            BigDecimal financeAmount,

            @Schema(description = "Total payable amount in SAR")
            BigDecimal totalPayable,

            @Schema(description = "Monthly installment in SAR")
            BigDecimal monthlyInstallment,

            @Schema(description = "Total installments count")
            int totalInstallments,

            @Schema(description = "Installments paid count")
            int installmentsPaid,

            @Schema(description = "Installments remaining count")
            int installmentsRemaining,

            @Schema(description = "Loan status")
            String status,

            @Schema(description = "Date loan was approved/disbursed")
            LocalDate dateApproved,

            @Schema(description = "Next installment due date")
            LocalDate nextInstallmentDueDate,

            @Schema(description = "Last installment due date")
            LocalDate lastInstallmentDueDate,

            @Schema(description = "Paid amount in SAR (for donut chart)")
            BigDecimal paidAmount,

            @Schema(description = "Pending amount in SAR (for donut chart)")
            BigDecimal pendingAmount,

            @Schema(description = "Overdue amount in SAR (for donut chart)")
            BigDecimal overdueAmount
    ) {}
}
