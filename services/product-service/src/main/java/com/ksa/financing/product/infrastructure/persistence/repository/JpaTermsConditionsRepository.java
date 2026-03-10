package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.TermsConditionsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_terms_conditions} table.
 */
@Repository
public interface JpaTermsConditionsRepository extends JpaRepository<TermsConditionsJpaEntity, UUID> {

    Optional<TermsConditionsJpaEntity> findByProductIdAndTenantId(UUID productId, UUID tenantId);
}
