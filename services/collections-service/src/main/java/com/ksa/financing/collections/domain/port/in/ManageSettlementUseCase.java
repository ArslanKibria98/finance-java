package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.SettlementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ManageSettlementUseCase {

    record SettlementQuote(
            UUID loanId,
            BigDecimal outstandingPrincipal,
            BigDecimal outstandingProfit,
            BigDecimal outstandingFees,
            BigDecimal totalOutstanding,
            BigDecimal ibraAmount,
            BigDecimal earlySettlementDiscount,  // discount from EARLY_SETTLEMENT DelinquencyRule (0 if none matched)
            BigDecimal settlementAmount,
            LocalDate quoteDate,
            LocalDate quoteExpiryDate
    ) {}

    record InitiateSettlementCommand(
            UUID tenantId,
            UUID loanId,
            UUID customerId,
            SettlementType settlementType,
            BigDecimal partialAmount,
            String idempotencyKey,
            UUID requestedBy
    ) {}

    record SettlementResponse(
            UUID settlementId,
            String settlementNumber,
            BigDecimal settlementAmount,
            BigDecimal ibraAmount,
            String status
    ) {}

    SettlementQuote getSettlementQuote(UUID tenantId, UUID loanId);

    SettlementResponse initiateSettlement(InitiateSettlementCommand command);

    SettlementResponse confirmSettlement(UUID tenantId, UUID settlementId, UUID paymentId);

    SettlementResponse getSettlement(UUID tenantId, UUID settlementId);
}
