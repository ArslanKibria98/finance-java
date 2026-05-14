package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record SimahReportResponse(
        String period,
        LocalDate generatedDate,
        List<SimahReportItem> items,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record SimahReportItem(
            UUID customerId,
            UUID loanId,
            String simahStatus,
            String facilityType,
            String paymentStatus
    ) {}
}
