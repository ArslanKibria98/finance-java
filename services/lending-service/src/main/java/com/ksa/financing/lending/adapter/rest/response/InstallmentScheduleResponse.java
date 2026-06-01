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

        @Schema(description = "Installment amount in SAR (same value as principalComponent under flat-principal reducing balance)")
        BigDecimal installmentAmount,

        @Schema(description = "Principal component in SAR (flat = P/n)")
        BigDecimal principalComponent,

        @Schema(description = "Profit component in SAR (declines monthly = outstanding × monthlyRate)")
        BigDecimal profitComponent,

        @Schema(description = "Fee component in SAR (declines proportionally with profit)")
        BigDecimal feeComponent,

        @Schema(description = "Total payable amount in SAR (principalComponent + profitComponent + feeComponent)")
        BigDecimal payableAmount,

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
    /**
     * Compact constructor — auto-derives payableAmount when null, so legacy 13-arg callers
     * still produce a correct value.
     */
    public InstallmentScheduleResponse {
        if (payableAmount == null) {
            BigDecimal p = principalComponent != null ? principalComponent : BigDecimal.ZERO;
            BigDecimal pr = profitComponent != null ? profitComponent : BigDecimal.ZERO;
            BigDecimal f = feeComponent != null ? feeComponent : BigDecimal.ZERO;
            payableAmount = p.add(pr).add(f);
        }
    }

    /**
     * Legacy 13-arg constructor (without payableAmount). payableAmount is auto-derived
     * by the compact constructor.
     */
    public InstallmentScheduleResponse(
            String invoiceId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal installmentAmount,
            BigDecimal principalComponent,
            BigDecimal profitComponent,
            BigDecimal feeComponent,
            BigDecimal outstandingBalance,
            String paymentStatus,
            LocalDate paidDate,
            BigDecimal paidAmount,
            boolean receiptAvailable,
            Map<String, Object> delinquency) {
        this(invoiceId, installmentNumber, dueDate, installmentAmount,
                principalComponent, profitComponent, feeComponent,
                null,  // payableAmount auto-derived
                outstandingBalance, paymentStatus, paidDate, paidAmount, receiptAvailable, delinquency);
    }

    public static InstallmentScheduleResponse of(
            String invoiceId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal installmentAmount,
            BigDecimal principalComponent,
            BigDecimal profitComponent,
            BigDecimal feeComponent,
            BigDecimal outstandingBalance,
            String paymentStatus,
            LocalDate paidDate,
            BigDecimal paidAmount,
            boolean receiptAvailable) {
        return new InstallmentScheduleResponse(invoiceId, installmentNumber, dueDate, installmentAmount,
                principalComponent, profitComponent, feeComponent, outstandingBalance, paymentStatus, paidDate,
                paidAmount, receiptAvailable, null);
    }
}
