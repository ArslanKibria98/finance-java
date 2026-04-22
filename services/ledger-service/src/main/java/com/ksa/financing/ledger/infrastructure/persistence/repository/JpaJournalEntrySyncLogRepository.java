package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntrySyncLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for journal_entry_sync_log table.
 */
public interface JpaJournalEntrySyncLogRepository extends JpaRepository<JournalEntrySyncLogJpaEntity, UUID> {

    Optional<JournalEntrySyncLogJpaEntity> findByTenantIdAndJournalEntryId(UUID tenantId, UUID journalEntryId);

    List<JournalEntrySyncLogJpaEntity> findAllBySyncStatus(String syncStatus);
}
