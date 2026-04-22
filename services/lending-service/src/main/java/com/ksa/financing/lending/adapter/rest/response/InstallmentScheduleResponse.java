package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

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
        boolean receiptAvailable,

        @Schema(description = "Delinquency snapshot mirrored from collections-service: status, dpd, latePenaltyAmount, earlySettlementEligible, discount fields, etc.")
        Map<String, Object> delinquency
) {
    public static InstallmentScheduleResponse of(
            String invoiceId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal installmentAmount,
            BigDecimal principalComponent,
            BigDecimal profitComponent,
            BigDecimal outstandingBalance,
            String paymentStatus,
            LocalDate paidDate,
            BigDecimal paidAmount,
            boolean receiptAvailable) {
        return new InstallmentScheduleResponse(invoiceId, installmentNumber, dueDate, installmentAmount,
                principalComponent, profitComponent, outstandingBalance, paymentStatus, paidDate,
                paidAmount, receiptAvailable, null);
    }
}
