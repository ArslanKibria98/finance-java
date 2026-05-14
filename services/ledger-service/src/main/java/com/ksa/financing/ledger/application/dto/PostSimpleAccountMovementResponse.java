package com.ksa.financing.ledger.application.dto;

import lombok.Builder;

/**
 * Result of posting a simple admin COA movement, optionally including a single-account ledger slice.
 */
@Builder
public record PostSimpleAccountMovementResponse(
        JournalEntryResponse journalEntry,
        LedgerReportResponse.AccountLedger ledgerSnapshot
) {}
