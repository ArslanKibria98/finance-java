package com.ksa.financing.ledger.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for reconciliation run results.
 */
public record ReconciliationReportResponse(
        UUID tenantId,
        LocalDate reconciliationDate,
        int totalAccountsChecked,
        int matchedAccounts,
        int discrepancyAccounts,
        List<AccountDiscrepancyDto> discrepancies
) {

    public record AccountDiscrepancyDto(
            UUID accountId,
            String accountCode,
            String accountName,
            BigDecimal internalBalance,
            BigDecimal fineractBalance,
            BigDecimal variance
    ) {}
}
