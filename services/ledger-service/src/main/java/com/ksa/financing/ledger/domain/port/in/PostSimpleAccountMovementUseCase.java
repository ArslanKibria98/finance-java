package com.ksa.financing.ledger.domain.port.in;

import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Input port: post a balanced two-line journal from a single COA movement against a configured offset account.
 */
public interface PostSimpleAccountMovementUseCase {

    SimpleAccountMovementResult post(SimpleAccountMovementCommand command);

    enum Movement {
        CREDIT_ON_ACCOUNT,
        DEBIT_ON_ACCOUNT
    }

    record SimpleAccountMovementCommand(
            UUID tenantId,
            String accountCode,
            UUID accountId,
            Movement movement,
            BigDecimal amount,
            LocalDate entryDate,
            String idempotencyKey,
            String description,
            String referenceType,
            UUID referenceId,
            UUID createdBy
    ) {}

    record SimpleAccountMovementResult(
            JournalEntryAggregate journalEntry,
            String targetAccountCode,
            UUID targetAccountId
    ) {}
}
