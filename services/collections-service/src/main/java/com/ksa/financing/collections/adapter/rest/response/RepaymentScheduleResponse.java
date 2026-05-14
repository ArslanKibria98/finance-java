package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RepaymentScheduleResponse(
        UUID id,
        UUID loanId,
        String scheduleNumber,
        BigDecimal totalPrincipal,
        BigDecimal totalProfit,
        BigDecimal totalFee,
        BigDecimal totalAmount,
        BigDecimal paidPrincipal,
        BigDecimal paidProfit,
        BigDecimal paidTotal,
        LocalDate firstDueDate,
        LocalDate lastDueDate,
        boolean fullyPaid,
        boolean earlySettlementEligible,
        LocalDateTime createdAt,
        List<InstallmentResponse> installments
) {}
