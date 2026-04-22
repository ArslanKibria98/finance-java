package com.ksa.financing.collections.application.dto;

import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDto(
        UUID id,
        UUID tenantId,
        String paymentNumber,
        UUID loanId,
        UUID customerId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        LocalDate valueDate,
        UUID sourceWalletId,
        String providerTransactionId,
        String idempotencyKey,
        boolean ledgerSynced,
        String failureCode,
        String failureMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
