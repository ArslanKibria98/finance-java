package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.FineractAccountMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for fineract_account_mappings table.
 */
public interface JpaFineractAccountMappingRepository
        extends JpaRepository<FineractAccountMappingJpaEntity, UUID> {

    Optional<FineractAccountMappingJpaEntity> findByTenantIdAndInternalAccountId(UUID tenantId, UUID internalAccountId);

    Optional<FineractAccountMappingJpaEntity> findByTenantIdAndInternalAccountCode(UUID tenantId, String internalAccountCode);

    List<FineractAccountMappingJpaEntity> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
