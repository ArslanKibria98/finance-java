package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalEntryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for journal_entries table.
 */
public interface JpaJournalEntryRepository extends JpaRepository<JournalEntryJpaEntity, UUID>, JpaSpecificationExecutor<JournalEntryJpaEntity> {

    Optional<JournalEntryJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<JournalEntryJpaEntity> findByTenantIdAndEntryNumber(UUID tenantId, String entryNumber);

    Optional<JournalEntryJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<JournalEntryJpaEntity> findByTenantIdAndReferenceTypeAndReferenceId(
            UUID tenantId, String referenceType, UUID referenceId);

    List<JournalEntryJpaEntity> findByTenantIdAndEntryDate(UUID tenantId, LocalDate entryDate);

    List<JournalEntryJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    @Query("SELECT e FROM JournalEntryJpaEntity e WHERE e.tenantId = :tenantId AND e.fineractSynced = false")
    List<JournalEntryJpaEntity> findUnsyncedByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(e) FROM JournalEntryJpaEntity e WHERE e.tenantId = :tenantId AND e.entryDate >= :startDate AND e.entryDate < :endDate")
    long countEntriesInPeriod(@Param("tenantId") UUID tenantId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    // ═════════════════════════════════════════════════════════════════════════════
    // NEW QUERY METHODS FOR REPORTS & FILTERING
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Find all entries for tenant in date range with pagination.
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.entryDate >= :fromDate AND e.entryDate <= :toDate " +
           "ORDER BY e.entryDate DESC, e.createdAt DESC")
    Page<JournalEntryJpaEntity> findByTenantAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    /**
     * Find all entries for tenant in date range (no pagination — for reports).
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.entryDate >= :fromDate AND e.entryDate <= :toDate " +
           "ORDER BY e.entryDate ASC, e.createdAt ASC")
    List<JournalEntryJpaEntity> findAllByTenantAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Find entries with optional reference-type and status filters (for vouchers report).
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.entryDate >= :fromDate AND e.entryDate <= :toDate " +
           "AND (:referenceType IS NULL OR e.referenceType = :referenceType) " +
           "AND (:status IS NULL OR CAST(e.status as string) = :status) " +
           "ORDER BY e.entryDate ASC, e.entryNumber ASC")
    List<JournalEntryJpaEntity> findForVouchersReport(
            @Param("tenantId") UUID tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("referenceType") String referenceType,
            @Param("status") String status);

    /**
     * Find entries by tenant and reference type (LOAN, REPAY, ACCRUAL, etc).
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId AND e.referenceType = :type " +
           "ORDER BY e.entryDate DESC, e.createdAt DESC")
    Page<JournalEntryJpaEntity> findByTenantAndType(
            @Param("tenantId") UUID tenantId,
            @Param("type") String type,
            Pageable pageable);

    /**
     * Find entries by tenant and status (POSTED, SUBMITTED, FAILED, PENDING_APPROVAL).
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId AND e.status = :status " +
           "ORDER BY e.entryDate DESC, e.createdAt DESC")
    Page<JournalEntryJpaEntity> findByTenantAndStatus(
            @Param("tenantId") UUID tenantId,
            @Param("status") String status,
            Pageable pageable);

    /**
     * Find entries by tenant and loan ID.
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId AND e.referenceId = :loanId " +
           "ORDER BY e.entryDate DESC, e.createdAt DESC")
    Page<JournalEntryJpaEntity> findByTenantAndLoan(
            @Param("tenantId") UUID tenantId,
            @Param("loanId") UUID loanId,
            Pageable pageable);

    /**
     * Count entries by tenant and status for a specific date.
     */
    @Query("SELECT e.status, COUNT(e) FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId AND DATE(e.entryDate) = :date " +
           "GROUP BY e.status")
    List<Object[]> countByTenantStatusOnDate(
            @Param("tenantId") UUID tenantId,
            @Param("date") LocalDate date);

    /**
     * Sum debits and credits for tenant on specific date.
     */
    @Query(value = "SELECT " +
           "  COALESCE(SUM(CAST(jl.debit_amount AS DECIMAL)), 0) as total_debits, " +
           "  COALESCE(SUM(CAST(jl.credit_amount AS DECIMAL)), 0) as total_credits " +
           "FROM journal_entries je " +
           "JOIN journal_lines jl ON je.id = jl.journal_entry_id " +
           "WHERE je.tenant_id = :tenantId AND DATE(je.entry_date) = :date",
           nativeQuery = true)
    Object[] sumDebitsCreditsByTenantOnDate(
            @Param("tenantId") UUID tenantId,
            @Param("date") LocalDate date);

    /**
     * Count failed entries for tenant.
     */
    @Query("SELECT COUNT(e) FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId AND e.status = 'FAILED'")
    long countFailedByTenant(@Param("tenantId") UUID tenantId);

    /**
     * Find all failed entries for tenant.
     */
    @Query("SELECT e FROM JournalEntryJpaEntity e " +
           "WHERE e.tenantId = :tenantId AND e.status = 'FAILED' " +
           "ORDER BY e.createdAt DESC")
    Page<JournalEntryJpaEntity> findFailedByTenant(
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    /**
     * Sum total amounts by reference type for date range.
     */
    @Query(value = "SELECT " +
           "  je.reference_type, " +
           "  COUNT(je.id) as entry_count, " +
           "  COALESCE(SUM(CAST(jl.debit_amount AS DECIMAL)), 0) as total_amount " +
           "FROM journal_entries je " +
           "JOIN journal_lines jl ON je.id = jl.journal_entry_id " +
           "WHERE je.tenant_id = :tenantId " +
           "  AND DATE(je.entry_date) >= :fromDate " +
           "  AND DATE(je.entry_date) <= :toDate " +
           "GROUP BY je.reference_type",
           nativeQuery = true)
    List<Object[]> sumByTypeInRange(
            @Param("tenantId") UUID tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
