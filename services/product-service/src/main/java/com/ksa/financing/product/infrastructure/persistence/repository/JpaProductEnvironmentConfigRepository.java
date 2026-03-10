package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ProductEnvironmentConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProductEnvironmentConfigRepository extends JpaRepository<ProductEnvironmentConfigJpaEntity, UUID> {

    List<ProductEnvironmentConfigJpaEntity> findByProductIdAndTenantId(UUID productId, UUID tenantId);

    Optional<ProductEnvironmentConfigJpaEntity> findByProductIdAndEnvironmentConfigIdAndTenantId(
            UUID productId, UUID environmentConfigId, UUID tenantId);

    void deleteByProductIdAndTenantId(UUID productId, UUID tenantId);
}
