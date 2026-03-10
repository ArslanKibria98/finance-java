package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.FeeSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_fee_settings} table.
 */
@Repository
public interface JpaFeeSettingsRepository extends JpaRepository<FeeSettingsJpaEntity, UUID> {

    Optional<FeeSettingsJpaEntity> findByProductIdAndTenantId(UUID productId, UUID tenantId);
}
