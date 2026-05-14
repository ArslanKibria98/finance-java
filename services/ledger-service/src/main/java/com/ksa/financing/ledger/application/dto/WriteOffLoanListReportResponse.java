package com.ksa.financing.ledger.application.dto;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record WriteOffLoanListReportResponse(
        String period,
        List<WriteOffLoanItem> items,
        @JsonIgnore PageMetadata pagination
) {
    @Builder
    public record WriteOffLoanItem(
            UUID loanId,
            UUID customerId,
            LocalDate writeOffDate,
            BigDecimal principalWrittenOff,
            BigDecimal provisionReleased
    ) {}
}
