package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EarlySettlementConfigResponse(
        UUID id,
        UUID delinquencyId,
        Integer invoiceOrder,
        int fromDay,
        int tillDay,
        boolean isPercentage,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        boolean isRange,
        int rangeNo,
        Integer minInvoiceOrder,
        Integer maxInvoiceOrder,
        int recordState,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
