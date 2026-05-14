package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DelinquencyRuleResponse(
        UUID id,
        UUID tenantId,
        UUID productId,
        int delinquencyType,            // wire code 1..6
        String delinquencyTypeName,     // enum name for clarity
        boolean isPercentage,
        BigDecimal penaltyPercentage,
        BigDecimal penaltyAmount,
        int fromDay,
        int tillDay,
        int penaltyType,
        int promisesPerYear,
        int promisesPerLoan,
        boolean isCustom,
        int settlementStrategy,
        String settlementDiscountType,
        int settlementMonths,
        BigDecimal settlementAmountPerMonth,
        String charityFundAccount,
        String channel,
        int recordState,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<EarlySettlementConfigResponse> earlySettlementConfigs) {
}
