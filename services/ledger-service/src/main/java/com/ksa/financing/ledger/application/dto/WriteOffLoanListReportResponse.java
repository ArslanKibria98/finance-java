package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record WriteOffLoanListReportResponse(
        String period,
        List<WriteOffLoanItem> items
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
