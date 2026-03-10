package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.DurationSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_duration_settings} table.
 */
@Repository
public interface JpaDurationSettingsRepository extends JpaRepository<DurationSettingsJpaEntity, UUID> {

    Optional<DurationSettingsJpaEntity> findByProductIdAndTenantId(UUID productId, UUID tenantId);
}
