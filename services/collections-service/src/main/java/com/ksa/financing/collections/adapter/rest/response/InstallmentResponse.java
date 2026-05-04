package com.ksa.financing.collections.adapter.rest.response;

import com.ksa.financing.collections.domain.model.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentResponse(
        UUID id,
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal principalAmount,
        BigDecimal profitAmount,
        BigDecimal feeAmount,
        BigDecimal latePenaltyAmount,
        BigDecimal totalAmount,
        BigDecimal paidPrincipal,
        BigDecimal paidProfit,
        BigDecimal paidFees,
        BigDecimal paidTotal,
        BigDecimal outstandingAmount,
        InstallmentStatus status,
        int dpd,
        // Early-settlement eligibility — resolved against active DelinquencyRule type=1
        boolean earlySettlementEligible,
        BigDecimal earlySettlementDiscountPercentage,
        BigDecimal earlySettlementDiscountAmount,
        Integer earlySettlementValidUntilDay,
        // Computed: absolute discount (percentage × outstanding, or flat amount)
        BigDecimal earlySettlementTotalDiscount,
        // Computed: totalAmount - earlySettlementTotalDiscount when eligible, else totalAmount
        BigDecimal payableAmount,
        // Write-off fields
        boolean isEligibleForWriteOff,
        BigDecimal writtenOffPrincipal,
        BigDecimal writtenOffProfit,
        BigDecimal writtenOffFee,
        BigDecimal writtenOffPenalty,
        LocalDate writeOffDate,
        String writeOffReason
) {}
