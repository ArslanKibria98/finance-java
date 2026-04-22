package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.AccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for accounts table.
 */
public interface JpaAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {

    Optional<AccountJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<AccountJpaEntity> findByTenantIdAndAccountCode(UUID tenantId, String accountCode);

    List<AccountJpaEntity> findAllByTenantId(UUID tenantId);

    List<AccountJpaEntity> findAllByTenantIdAndAccountType(UUID tenantId, String accountType);

    boolean existsByTenantIdAndAccountCode(UUID tenantId, String accountCode);
}
