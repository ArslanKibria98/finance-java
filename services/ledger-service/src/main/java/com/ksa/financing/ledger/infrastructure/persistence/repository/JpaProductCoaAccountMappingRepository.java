package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.infrastructure.persistence.entity.ProductCoaAccountMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProductCoaAccountMappingRepository extends JpaRepository<ProductCoaAccountMappingJpaEntity, UUID> {

    List<ProductCoaAccountMappingJpaEntity> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    Optional<ProductCoaAccountMappingJpaEntity> findByTenantIdAndProductIdAndCoaFieldId(
            UUID tenantId, UUID productId, UUID coaFieldId);

    List<ProductCoaAccountMappingJpaEntity> findByTenantIdAndCoaFieldId(UUID tenantId, UUID coaFieldId);

    List<ProductCoaAccountMappingJpaEntity> findByTenantIdAndAccountId(UUID tenantId, UUID accountId);

    void deleteByTenantIdAndProductIdAndCoaFieldId(UUID tenantId, UUID productId, UUID coaFieldId);
}
