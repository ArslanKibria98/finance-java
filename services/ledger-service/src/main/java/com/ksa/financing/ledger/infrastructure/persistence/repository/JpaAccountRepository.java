package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for accounts table.
 */
public interface JpaAccountRepository extends JpaRepository<AccountJpaEntity, UUID>, JpaSpecificationExecutor<AccountJpaEntity> {

    Optional<AccountJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<AccountJpaEntity> findByTenantIdAndAccountCode(UUID tenantId, String accountCode);

    List<AccountJpaEntity> findAllByTenantId(UUID tenantId);

    Page<AccountJpaEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    List<AccountJpaEntity> findAllByTenantIdAndAccountType(UUID tenantId, String accountType);

    boolean existsByTenantIdAndAccountCode(UUID tenantId, String accountCode);

    /**
     * Paginated tenant lookup with optional case-insensitive multi-field search.
     * When {@code search} is null/blank caller should pass null — the JPQL guards skip the LIKE branch.
     * Search pattern format: {@code "%lowercased-term%"}.
     */
    @Query("SELECT a FROM AccountJpaEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:search IS NULL OR " +
            "     LOWER(a.accountCode) LIKE :search OR " +
            "     LOWER(a.accountName) LIKE :search OR " +
            "     LOWER(COALESCE(a.accountNameAr, '')) LIKE :search)")
    Page<AccountJpaEntity> searchByTenant(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            Pageable pageable);
}
