package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageDelinquencyRuleUseCase {

    enum ConfigKind { SINGLE, RANGE }

    record ConfigItem(
            ConfigKind kind,
            boolean isPercentage,
            BigDecimal discountPercentage,
            BigDecimal discountAmount,
            int fromDay,
            int tillDay,
            Integer invoiceOrder,
            Integer rangeNo,
            Integer minInvoiceOrder,
            Integer maxInvoiceOrder) {}

    record UpsertRuleCommand(
            UUID tenantId,
            UUID productId,
            DelinquencyType delinquencyType,
            boolean isPercentage,
            BigDecimal penaltyPercentage,
            BigDecimal penaltyAmount,
            int fromDay,
            int tillDay,
            int penaltyType,
            int promisesPerYear,
            int promisesPerLoan,
            boolean isCustom,
            com.ksa.financing.collections.domain.model.EarlySettlementStrategy settlementStrategy,
            String settlementDiscountType,
            int settlementMonths,
            BigDecimal settlementAmountPerMonth,
            String charityFundAccount,
            List<ConfigItem> configs) {}

    DelinquencyRule upsertRule(UpsertRuleCommand cmd);

    void softDeleteRule(UUID tenantId, UUID ruleId);

    List<DelinquencyRule> listForProduct(UUID tenantId, UUID productId);

    DelinquencyRule getById(UUID tenantId, UUID ruleId);
}
