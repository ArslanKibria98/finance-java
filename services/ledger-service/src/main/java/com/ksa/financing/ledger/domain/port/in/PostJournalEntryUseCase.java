package com.ksa.financing.ledger.domain.port.in;

import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalLine;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Input port: Post a balanced journal entry to the ledger.
 * Idempotent — duplicate calls with the same idempotencyKey return the cached result.
 */
public interface PostJournalEntryUseCase {

    JournalEntryAggregate post(PostJournalEntryCommand command);

    /**
     * Command carrying all data needed to post a journal entry.
     */
    record PostJournalEntryCommand(
            UUID tenantId,
            LocalDate entryDate,
            String referenceType,
            UUID referenceId,
            String transactionType,
            String description,
            List<JournalLine> lines,
            String idempotencyKey,
            UUID createdBy
    ) {
        public PostJournalEntryCommand {
            if (tenantId == null) throw new IllegalArgumentException("TenantId is required");
            if (entryDate == null) throw new IllegalArgumentException("Entry date is required");
            if (referenceType == null || referenceType.isBlank()) throw new IllegalArgumentException("ReferenceType is required");
            if (referenceId == null) throw new IllegalArgumentException("ReferenceId is required");
            if (description == null || description.isBlank()) throw new IllegalArgumentException("Description is required");
            if (lines == null || lines.size() < 2) throw new IllegalArgumentException("At least 2 journal lines required");
            if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("IdempotencyKey is required");
        }
    }
}
