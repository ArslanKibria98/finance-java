package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BRD UC#04 — Installment schedule entry for Finance Overview.
 */
@Schema(description = "Individual installment in the repayment schedule")
public record InstallmentScheduleResponse(

        @Schema(description = "Unique invoice ID for this installment")
        String invoiceId,

        @Schema(description = "Installment number (1-based)")
        int installmentNumber,

        @Schema(description = "Installment due date")
        LocalDate dueDate,

        @Schema(description = "Installment amount in SAR")
        BigDecimal installmentAmount,

        @Schema(description = "Principal component in SAR")
        BigDecimal principalComponent,

        @Schema(description = "Profit component in SAR")
        BigDecimal profitComponent,

        @Schema(description = "Outstanding balance after payment in SAR")
        BigDecimal outstandingBalance,

        @Schema(description = "Payment status: PAID, PENDING, OVERDUE")
        String paymentStatus,

        @Schema(description = "Date payment was made (null if not paid)")
        LocalDate paidDate,

        @Schema(description = "Amount paid (null if not paid)")
        BigDecimal paidAmount,

        @Schema(description = "Receipt available for download")
        boolean receiptAvailable
) {}
