package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ProductDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_documents} table.
 */
@Repository
public interface JpaProductDocumentRepository extends JpaRepository<ProductDocumentJpaEntity, UUID> {

    List<ProductDocumentJpaEntity> findByProductIdAndTenantIdOrderBySortOrder(UUID productId, UUID tenantId);

    Optional<ProductDocumentJpaEntity> findByIdAndProductIdAndTenantId(UUID id, UUID productId, UUID tenantId);

    boolean existsByIdAndProductIdAndTenantId(UUID id, UUID productId, UUID tenantId);

    void deleteByIdAndProductIdAndTenantId(UUID id, UUID productId, UUID tenantId);
}
