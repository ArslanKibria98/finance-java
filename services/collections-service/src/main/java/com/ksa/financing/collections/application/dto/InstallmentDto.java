package com.ksa.financing.collections.application.dto;

import com.ksa.financing.collections.domain.model.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentDto(
        UUID id,
        UUID scheduleId,
        UUID loanId,
        int installmentNumber,
        LocalDate dueDate,
        BigDecimal principalAmount,
        BigDecimal profitAmount,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        BigDecimal paidPrincipal,
        BigDecimal paidProfit,
        BigDecimal paidFee,
        BigDecimal paidTotal,
        BigDecimal outstandingAmount,
        InstallmentStatus status,
        int dpd,
        LocalDate paidDate
) {}
