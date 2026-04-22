package com.ksa.financing.collections.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpsertDelinquencyRuleRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer delinquencyType,   // 1..6 (see DelinquencyType.code())
        Boolean isPercentage,
        BigDecimal penaltyPercentage,
        BigDecimal penaltyAmount,
        Integer fromDay,
        Integer tillDay,
        Integer penaltyType,
        Integer promisesPerYear,
        Integer promisesPerLoan,
        Boolean isCustom,
        String charityFundAccount,
        @Valid List<ConfigItem> configs) {

    public enum ConfigKind { SINGLE, RANGE }

    public record ConfigItem(
            @NotNull ConfigKind kind,
            @NotNull Boolean isPercentage,
            BigDecimal discountPercentage,
            BigDecimal discountAmount,
            @NotNull Integer fromDay,
            @NotNull Integer tillDay,
            Integer invoiceOrder,
            Integer rangeNo,
            Integer minInvoiceOrder,
            Integer maxInvoiceOrder) {
    }
}
