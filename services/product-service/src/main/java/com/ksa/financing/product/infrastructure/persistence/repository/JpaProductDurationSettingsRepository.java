package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ProductDurationSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProductDurationSettingsRepository extends JpaRepository<ProductDurationSettingsJpaEntity, UUID> {

    Optional<ProductDurationSettingsJpaEntity> findByProductIdAndTenantId(UUID productId, UUID tenantId);
}
