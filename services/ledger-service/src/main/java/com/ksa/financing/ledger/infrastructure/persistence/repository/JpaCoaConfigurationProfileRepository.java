package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaConfigurationProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCoaConfigurationProfileRepository extends JpaRepository<CoaConfigurationProfileJpaEntity, UUID> {

    Optional<CoaConfigurationProfileJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<CoaConfigurationProfileJpaEntity> findAllByTenantIdAndProductCodeOrderByCreatedAtDesc(UUID tenantId, String productCode);
}
