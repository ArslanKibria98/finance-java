package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code products} table.
 * All queries filter by {@code deleted_at IS NULL} to respect soft-delete.
 */
@Repository
public interface JpaProductRepository
        extends JpaRepository<ProductJpaEntity, UUID>,
                JpaSpecificationExecutor<ProductJpaEntity> {

    Optional<ProductJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<ProductJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<ProductJpaEntity> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);

    boolean existsByProductCodeAndTenantIdAndDeletedAtIsNull(String productCode, UUID tenantId);

    Optional<ProductJpaEntity> findByProductCodeAndTenantIdAndDeletedAtIsNull(String productCode, UUID tenantId);
}
