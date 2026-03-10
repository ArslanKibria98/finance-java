package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record UpdateAdminFeeSlabsRequest(
    @NotEmpty @Valid List<AdminFeeSlabItem> slabs
) {
    public record AdminFeeSlabItem(
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal profitPercentage,
        BigDecimal processingFee,
        BigDecimal adminFee,
        String partnerScope,
        String status,
        int sortOrder,
        Integer minTenure,
        Integer maxTenure
    ) {}
}
