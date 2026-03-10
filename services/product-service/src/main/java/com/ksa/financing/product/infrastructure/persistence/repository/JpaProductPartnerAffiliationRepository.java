package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ProductPartnerAffiliationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_partner_affiliations} table.
 */
@Repository
public interface JpaProductPartnerAffiliationRepository
        extends JpaRepository<ProductPartnerAffiliationJpaEntity, UUID> {

    boolean existsByProductIdAndPartnerId(UUID productId, UUID partnerId);

    void deleteByProductIdAndPartnerId(UUID productId, UUID partnerId);
}
