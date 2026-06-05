package com.ksa.financing.ledger.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.domain.model.EntryStatus;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: persistence contract for JournalEntryAggregate.
 * Implementations live in infrastructure.persistence.
 */
public interface JournalEntryRepository {

    JournalEntryAggregate save(JournalEntryAggregate entry);

    Optional<JournalEntryAggregate> findById(UUID tenantId, JournalEntryId id);

    Optional<JournalEntryAggregate> findByEntryNumber(UUID tenantId, String entryNumber);

    Optional<JournalEntryAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey);

    PageResponse<JournalEntryAggregate> findByReference(UUID tenantId, String referenceType, UUID referenceId, PageQuery query);

    PageResponse<JournalEntryAggregate> findByDate(UUID tenantId, LocalDate entryDate, PageQuery query);

    PageResponse<JournalEntryAggregate> findByStatus(UUID tenantId, EntryStatus status, PageQuery query);

    PageResponse<JournalEntryAggregate> findAllByTenant(UUID tenantId, PageQuery query);

    List<JournalEntryAggregate> findUnsynced(UUID tenantId);

    /**
     * Generate the next sequential entry number for the tenant.
     * Format: JE-{YYYYMM}-{sequence}
     */
    String generateEntryNumber(UUID tenantId, LocalDate entryDate);
}
