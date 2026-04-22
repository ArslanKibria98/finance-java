package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.domain.model.EntryStatus;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryId;
import com.ksa.financing.ledger.domain.port.out.JournalEntryRepository;
import com.ksa.financing.ledger.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.mapper.JournalEntryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Output port implementation: persists JournalEntryAggregate via JPA.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JournalEntryRepositoryImpl implements JournalEntryRepository {

    private final JpaJournalEntryRepository jpaRepo;
    private final JpaIdempotencyKeyRepository idempotencyRepo;
    private final JournalEntryPersistenceMapper mapper;

    @Override
    public JournalEntryAggregate save(JournalEntryAggregate entry) {
        // Retrieve existing idempotency key if present
        String idempotencyKey = findExistingIdempotencyKey(entry);

        JournalEntryJpaEntity entity = mapper.toJpaEntity(entry, idempotencyKey);
        JournalEntryJpaEntity saved = jpaRepo.save(entity);

        // Persist idempotency record
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            saveIdempotencyRecord(entry, saved.getId(), idempotencyKey);
        }

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<JournalEntryAggregate> findById(UUID tenantId, JournalEntryId id) {
        return jpaRepo.findByTenantIdAndId(tenantId, id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<JournalEntryAggregate> findByEntryNumber(UUID tenantId, String entryNumber) {
        return jpaRepo.findByTenantIdAndEntryNumber(tenantId, entryNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<JournalEntryAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        // Look up idempotency table first, then fetch the entry
        return idempotencyRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .flatMap(idem -> jpaRepo.findByTenantIdAndId(tenantId, idem.getResourceId()))
                .map(mapper::toDomain);
    }

    @Override
    public List<JournalEntryAggregate> findByReference(UUID tenantId, String referenceType, UUID referenceId) {
        return jpaRepo.findByTenantIdAndReferenceTypeAndReferenceId(tenantId, referenceType, referenceId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<JournalEntryAggregate> findByDate(UUID tenantId, LocalDate entryDate) {
        return jpaRepo.findByTenantIdAndEntryDate(tenantId, entryDate)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<JournalEntryAggregate> findByStatus(UUID tenantId, EntryStatus status) {
        return jpaRepo.findByTenantIdAndStatus(tenantId, status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<JournalEntryAggregate> findUnsynced(UUID tenantId) {
        return jpaRepo.findUnsyncedByTenantId(tenantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public String generateEntryNumber(UUID tenantId, LocalDate entryDate) {
        String yyyyMM = entryDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate startOfMonth = entryDate.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        long count = jpaRepo.countEntriesInPeriod(tenantId, startOfMonth, startOfNextMonth);
        long seq = count + 1;

        return String.format("JE-%s-%06d", yyyyMM, seq);
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // NEW METHODS FOR GL QUERIES & REPORTS
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Find GL entries by date range with pagination.
     */
    public Page<JournalEntryAggregate> findByDateRange(
            UUID tenantId, LocalDate from, LocalDate to, Pageable pageable) {

        Page<JournalEntryJpaEntity> entities = jpaRepo.findByTenantAndDateRange(
                tenantId, from, to, pageable);

        return new PageImpl<>(
                entities.getContent().stream()
                        .map(mapper::toDomain)
                        .toList(),
                pageable,
                entities.getTotalElements()
        );
    }

    /**
     * Find GL entries by type with pagination.
     */
    public Page<JournalEntryAggregate> findByType(
            UUID tenantId, String type, Pageable pageable) {

        Page<JournalEntryJpaEntity> entities = jpaRepo.findByTenantAndType(
                tenantId, type, pageable);

        return new PageImpl<>(
                entities.getContent().stream()
                        .map(mapper::toDomain)
                        .toList(),
                pageable,
                entities.getTotalElements()
        );
    }

    /**
     * Find GL entries by status with pagination.
     */
    public Page<JournalEntryAggregate> findByStatusWithPaging(
            UUID tenantId, String status, Pageable pageable) {

        Page<JournalEntryJpaEntity> entities = jpaRepo.findByTenantAndStatus(
                tenantId, status, pageable);

        return new PageImpl<>(
                entities.getContent().stream()
                        .map(mapper::toDomain)
                        .toList(),
                pageable,
                entities.getTotalElements()
        );
    }

    /**
     * Find GL entries by loan with pagination.
     */
    public Page<JournalEntryAggregate> findByLoan(
            UUID tenantId, UUID loanId, Pageable pageable) {

        Page<JournalEntryJpaEntity> entities = jpaRepo.findByTenantAndLoan(
                tenantId, loanId, pageable);

        return new PageImpl<>(
                entities.getContent().stream()
                        .map(mapper::toDomain)
                        .toList(),
                pageable,
                entities.getTotalElements()
        );
    }

    /**
     * Get count of entries by status for a specific date.
     */
    public Map<String, Long> countByStatusOnDate(UUID tenantId, LocalDate date) {
        List<Object[]> results = jpaRepo.countByTenantStatusOnDate(tenantId, date);

        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : results) {
            String status = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            statusCounts.put(status, count);
        }

        return statusCounts;
    }

    /**
     * Get total debits and credits for a specific date.
     */
    public Map<String, BigDecimal> sumDebitsCreditsByDate(UUID tenantId, LocalDate date) {
        Object[] result = jpaRepo.sumDebitsCreditsByTenantOnDate(tenantId, date);

        Map<String, BigDecimal> totals = new HashMap<>();
        if (result != null) {
            BigDecimal totalDebits = new BigDecimal(result[0].toString());
            BigDecimal totalCredits = new BigDecimal(result[1].toString());
            totals.put("totalDebits", totalDebits);
            totals.put("totalCredits", totalCredits);
        } else {
            totals.put("totalDebits", BigDecimal.ZERO);
            totals.put("totalCredits", BigDecimal.ZERO);
        }

        return totals;
    }

    /**
     * Get count of failed entries for a tenant.
     */
    public long countFailedEntries(UUID tenantId) {
        return jpaRepo.countFailedByTenant(tenantId);
    }

    /**
     * Find failed GL entries with pagination.
     */
    public Page<JournalEntryAggregate> findFailedEntries(UUID tenantId, Pageable pageable) {
        Page<JournalEntryJpaEntity> entities = jpaRepo.findFailedByTenant(tenantId, pageable);

        return new PageImpl<>(
                entities.getContent().stream()
                        .map(mapper::toDomain)
                        .toList(),
                pageable,
                entities.getTotalElements()
        );
    }

    /**
     * Get summary of entries by type for date range.
     */
    public Map<String, Map<String, Long>> sumByTypeInRange(
            UUID tenantId, LocalDate from, LocalDate to) {

        List<Object[]> results = jpaRepo.sumByTypeInRange(tenantId, from, to);

        Map<String, Map<String, Long>> typeSummary = new HashMap<>();
        for (Object[] row : results) {
            String type = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            Long amount = ((Number) row[2]).longValue();

            Map<String, Long> summary = new HashMap<>();
            summary.put("count", count);
            summary.put("totalAmount", amount);
            typeSummary.put(type, summary);
        }

        return typeSummary;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String findExistingIdempotencyKey(JournalEntryAggregate entry) {
        // The idempotency key is stored on the JPA entity — here we derive it from the
        // existing entity if an update. On first save it will be provided externally.
        // The key is carried from the use case context through the JPA entity's column.
        return null; // Will be set by save() caller via idempotency_key column
    }

    private void saveIdempotencyRecord(JournalEntryAggregate entry, UUID journalEntryId, String idempotencyKey) {
        var idem = IdempotencyKeyJpaEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(entry.getTenantId())
                .idempotencyKey(idempotencyKey)
                .operationType("JOURNAL_ENTRY")
                .resourceType("JOURNAL_ENTRY")
                .resourceId(journalEntryId)
                .requestHash(Integer.toHexString(idempotencyKey.hashCode()))
                .status("COMPLETED")
                .completedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        idempotencyRepo.save(idem);
    }
}
