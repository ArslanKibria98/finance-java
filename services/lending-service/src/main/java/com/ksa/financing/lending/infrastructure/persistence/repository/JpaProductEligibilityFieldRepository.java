package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.infrastructure.persistence.entity.ProductEligibilityFieldJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaProductEligibilityFieldRepository extends JpaRepository<ProductEligibilityFieldJpaEntity, UUID> {

    List<ProductEligibilityFieldJpaEntity> findByTenantIdAndProductIdOrderBySortOrder(UUID tenantId, UUID productId);

    void deleteByTenantIdAndId(UUID tenantId, UUID id);

    void deleteByTenantIdAndProductId(UUID tenantId, UUID productId);
}
