package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.MasterCategoryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_master_categories} table.
 */
@Repository
public interface JpaMasterCategoryRepository extends JpaRepository<MasterCategoryJpaEntity, UUID>, JpaSpecificationExecutor<MasterCategoryJpaEntity> {

    List<MasterCategoryJpaEntity> findAllByTenantId(UUID tenantId);
    Page<MasterCategoryJpaEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    Optional<MasterCategoryJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MasterCategoryJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);
}
