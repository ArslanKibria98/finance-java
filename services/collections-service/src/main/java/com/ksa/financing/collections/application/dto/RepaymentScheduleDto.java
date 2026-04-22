package com.ksa.financing.collections.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RepaymentScheduleDto(
        UUID id,
        UUID tenantId,
        String scheduleNumber,
        UUID loanId,
        int version,
        boolean active,
        int totalInstallments,
        BigDecimal totalPrincipal,
        BigDecimal totalProfit,
        BigDecimal totalAmount,
        BigDecimal totalOutstanding,
        LocalDate firstDueDate,
        LocalDate lastDueDate,
        List<InstallmentDto> installments,
        LocalDateTime createdAt
) {}
