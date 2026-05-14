package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for account_balances table.
 */
public interface JpaAccountBalanceRepository extends JpaRepository<AccountBalanceJpaEntity, UUID> {

    java.util.List<AccountBalanceJpaEntity> findAllByTenantIdAndBalanceDate(UUID tenantId, LocalDate date);

    Optional<AccountBalanceJpaEntity> findByTenantIdAndAccountIdAndBalanceDate(
            UUID tenantId, UUID accountId, LocalDate date);

    /**
     * Find the latest balance row per account for a tenant (regardless of date).
     * Used by trial balance when no date filter is supplied — returns all accounts'
     * most recent balance snapshot.
     */
    @Query("SELECT ab FROM AccountBalanceJpaEntity ab " +
           "WHERE ab.tenantId = :tenantId " +
           "AND ab.balanceDate = (" +
           "    SELECT MAX(ab2.balanceDate) FROM AccountBalanceJpaEntity ab2 " +
           "    WHERE ab2.tenantId = :tenantId AND ab2.accountId = ab.accountId" +
           ")")
    java.util.List<AccountBalanceJpaEntity> findLatestBalancesPerAccount(@Param("tenantId") UUID tenantId);

    /**
     * Find the most recent balance before a given date.
     */
    @Query("SELECT ab FROM AccountBalanceJpaEntity ab " +
           "WHERE ab.tenantId = :tenantId " +
           "AND ab.accountId = :accountId " +
           "AND ab.balanceDate < :date " +
           "ORDER BY ab.balanceDate DESC " +
           "LIMIT 1")
    Optional<AccountBalanceJpaEntity> findMostRecentBalanceBefore(
            @Param("tenantId") UUID tenantId,
            @Param("accountId") UUID accountId,
            @Param("date") LocalDate date);
}
