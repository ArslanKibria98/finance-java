package com.ksa.financing.lending.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for LoanApplicationAggregate. Immutable Java record.
 */
public record LoanApplicationDto(
        String id,
        String tenantId,
        String applicationNumber,
        String customerId,
        String productId,
        String productCode,
        String shariaStructure,
        BigDecimal requestedAmount,
        int requestedTenureMonths,
        String partnerId,
        String leadId,
        BigDecimal approvedAmount,
        Integer approvedTenureMonths,
        BigDecimal approvedProfitRate,
        BigDecimal totalProfit,
        BigDecimal totalRepayment,
        BigDecimal monthlyInstallment,
        BigDecimal dbrBefore,
        BigDecimal dbrAfter,
        String status,
        String workflowId,
        String currentStage,
        LocalDateTime submittedAt,
        LocalDateTime expiresAt,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        int version
) {}
