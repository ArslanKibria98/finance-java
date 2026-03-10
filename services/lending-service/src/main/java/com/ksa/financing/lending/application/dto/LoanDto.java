package com.ksa.financing.lending.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for LoanAggregate. Immutable Java record.
 */
public record LoanDto(
        String id,
        String tenantId,
        String loanNumber,
        String applicationId,
        String customerId,
        String productId,
        String productCode,
        String shariaStructure,
        String commodityTransactionId,
        BigDecimal principalAmount,
        BigDecimal profitAmount,
        BigDecimal totalAmount,
        BigDecimal profitRate,
        int tenureMonths,
        BigDecimal installmentAmount,
        BigDecimal outstandingPrincipal,
        BigDecimal outstandingProfit,
        BigDecimal outstandingFees,
        BigDecimal totalOutstanding,
        String status,
        LocalDate bookingDate,
        LocalDate disbursementDate,
        LocalDate firstDueDate,
        LocalDate maturityDate,
        LocalDate settlementDate,
        int currentDpd,
        int maxDpd,
        int ifrs9Stage,
        Long fineractLoanId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version
) {}
