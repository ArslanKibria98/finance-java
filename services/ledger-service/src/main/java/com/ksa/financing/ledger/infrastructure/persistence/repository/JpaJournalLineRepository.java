package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.JournalLineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for journal_lines table.
 */
public interface JpaJournalLineRepository extends JpaRepository<JournalLineJpaEntity, UUID> {

    List<JournalLineJpaEntity> findByTenantIdAndAccountId(UUID tenantId, UUID accountId);

    List<JournalLineJpaEntity> findByTenantIdAndJournalEntry_EntryDate(UUID tenantId, LocalDate entryDate);

    /**
     * Find all journal lines for an account up to a specific date.
     */
    @Query("SELECT jl FROM JournalLineJpaEntity jl " +
           "JOIN jl.journalEntry je " +
           "WHERE jl.tenantId = :tenantId " +
           "AND jl.accountId = :accountId " +
           "AND je.entryDate <= :date " +
           "ORDER BY je.entryDate ASC, jl.lineNumber ASC")
    List<JournalLineJpaEntity> findByAccountUpToDate(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("date") LocalDate date);

    /**
     * Sum debits and credits for an account up to a date.
     */
    @Query(value = "SELECT " +
           "  COALESCE(SUM(CAST(jl.debit_amount AS DECIMAL)), 0) as total_debits, " +
           "  COALESCE(SUM(CAST(jl.credit_amount AS DECIMAL)), 0) as total_credits " +
           "FROM journal_lines jl " +
           "JOIN journal_entry_jpa_entity je ON jl.journal_entry_id = je.id " +
           "WHERE jl.tenant_id = :tenantId " +
           "AND jl.account_id = :accountId " +
           "AND je.entry_date <= :date",
           nativeQuery = true)
    Object[] sumDebitsCreditsByAccountUpToDate(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("date") LocalDate date);

    /**
     * Sum debits and credits for an account strictly before a date (used for opening balance).
     */
    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0), COALESCE(SUM(jl.creditAmount), 0) " +
           "FROM JournalLineJpaEntity jl " +
           "JOIN jl.journalEntry je " +
           "WHERE jl.tenantId = :tenantId " +
           "AND jl.accountId = :accountId " +
           "AND je.entryDate < :date " +
           "AND je.status = 'POSTED'")
    Object[] sumDebitsCreditsBefore(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("date") LocalDate date);

    /**
     * Fetch all lines for an account within a date range (inclusive), oldest first.
     */
    @Query("SELECT jl FROM JournalLineJpaEntity jl " +
           "JOIN FETCH jl.journalEntry je " +
           "WHERE jl.tenantId = :tenantId " +
           "AND jl.accountId = :accountId " +
           "AND je.entryDate >= :fromDate AND je.entryDate <= :toDate " +
           "ORDER BY je.entryDate ASC, je.createdAt ASC, jl.lineNumber ASC")
    List<JournalLineJpaEntity> findByAccountInDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Sum debits and credits for an account within a date range (inclusive).
     * Returns [totalDebits, totalCredits].
     */
    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0), COALESCE(SUM(jl.creditAmount), 0) " +
           "FROM JournalLineJpaEntity jl " +
           "JOIN jl.journalEntry je " +
           "WHERE jl.tenantId = :tenantId " +
           "AND jl.accountId = :accountId " +
           "AND je.entryDate >= :fromDate AND je.entryDate <= :toDate " +
           "AND je.status = 'POSTED'")
    Object[] sumDebitsCreditsByAccountInRange(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Sum debits and credits for an account joined with journal entries filtered by transaction_type.
     * Used to categorize cash flow entries by business event type.
     */
    @Query(value = "SELECT " +
           "  je.transaction_type, " +
           "  COALESCE(SUM(CAST(jl.debit_amount AS DECIMAL)), 0) as total_debits, " +
           "  COALESCE(SUM(CAST(jl.credit_amount AS DECIMAL)), 0) as total_credits, " +
           "  COUNT(DISTINCT je.id) as entry_count " +
           "FROM journal_lines jl " +
           "JOIN journal_entries je ON jl.journal_entry_id = je.id " +
           "WHERE jl.tenant_id = :tenantId " +
           "AND jl.account_id = :accountId " +
           "AND je.entry_date >= :fromDate AND je.entry_date <= :toDate " +
           "GROUP BY je.transaction_type",
           nativeQuery = true)
    List<Object[]> sumByTransactionTypeForAccountInRange(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
