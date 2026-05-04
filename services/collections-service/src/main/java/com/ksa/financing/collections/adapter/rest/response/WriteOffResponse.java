package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record WriteOffResponse(
        UUID id,
        UUID tenantId,
        UUID loanId,
        UUID scheduleId,
        UUID installmentId,
        UUID delinquencyRuleId,
        BigDecimal principalAmount,
        BigDecimal profitAmount,
        BigDecimal feeAmount,
        BigDecimal penaltyAmount,
        BigDecimal totalAmount,
        int dpdAtWriteOff,
        String triggerType,
        String reason,
        String approvalReference,
        String status,
        String reversalReason,
        LocalDateTime reversedAt,
        UUID reversedBy,
        LocalDate writeOffDate,
        UUID initiatedBy,
        LocalDateTime createdAt
) {}
