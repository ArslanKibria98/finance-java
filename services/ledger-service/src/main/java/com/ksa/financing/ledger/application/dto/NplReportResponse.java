package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record NplReportResponse(
        LocalDate asOfDate,
        BigDecimal totalOutstanding,
        BigDecimal nplOutstanding,
        BigDecimal nplRatio,
        List<NplBucket> buckets
) {
    @Builder
    public record NplBucket(
            String bucket,
            BigDecimal outstanding
    ) {}
}
