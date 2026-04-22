package com.ksa.financing.ledger.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Input port: Run daily GL reconciliation against Fineract balances.
 */
public interface RunReconciliationUseCase {

    ReconciliationSummary run(UUID tenantId, LocalDate reconciliationDate);

    /**
     * Result summary of a reconciliation run.
     */
    record ReconciliationSummary(
            UUID tenantId,
            LocalDate reconciliationDate,
            int totalAccountsChecked,
            int matchedAccounts,
            int discrepancyAccounts,
            List<AccountDiscrepancy> discrepancies
    ) {}

    record AccountDiscrepancy(
            UUID accountId,
            String accountCode,
            String accountName,
            BigDecimal internalBalance,
            BigDecimal fineractBalance,
            BigDecimal variance
    ) {}
}
