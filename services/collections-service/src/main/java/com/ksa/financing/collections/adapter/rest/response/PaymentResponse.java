package com.ksa.financing.collections.adapter.rest.response;

import com.ksa.financing.collections.domain.model.PaymentMethod;
import com.ksa.financing.collections.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID loanId,
        UUID installmentId,
        String invoiceId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String idempotencyKey,
        String providerReference,
        String providerTransactionId,
        String failureCode,
        String failureMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
