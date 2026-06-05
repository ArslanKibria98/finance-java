package com.ksa.financing.ledger.domain.port.out;

import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Output port: Fineract GL synchronization contract.
 * Implementations live in infrastructure.fineract.
 */
public interface FineractSyncPort {

    /**
     * Post a journal entry to Fineract GL module.
     *
     * @param entry the domain entry to sync
     * @return Fineract transaction ID on success
     */
    Long postJournalEntry(JournalEntryAggregate entry);

    /**
     * Fetch current balance for a GL account from Fineract.
     *
     * @param fineractGlAccountId Fineract's internal GL account ID
     * @return current balance as BigDecimal
     */
    BigDecimal getAccountBalance(Long fineractGlAccountId);

    /**
     * Check if Fineract is reachable.
     */
    boolean isHealthy();
}
