package com.ksa.financing.collections.adapter.rest.response;

import com.ksa.financing.collections.domain.model.SettlementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementQuoteResponse(
        UUID loanId,
        SettlementType settlementType,
        LocalDate settlementDate,
        BigDecimal outstandingPrincipal,
        BigDecimal accruedProfit,
        BigDecimal unearnedProfit,
        BigDecimal ibraWaiver,
        BigDecimal earlySettlementDiscount,   // admin DelinquencyRule type=1 discount applied on top of Ibra
        BigDecimal outstandingFees,
        BigDecimal totalSettlementAmount,
        LocalDateTime quoteGeneratedAt,
        LocalDateTime quoteExpiresAt
) {}
