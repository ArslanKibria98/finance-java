package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.SubCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface for the {@code product_sub_categories} table.
 */
@Repository
public interface JpaSubCategoryRepository extends JpaRepository<SubCategoryJpaEntity, UUID>, JpaSpecificationExecutor<SubCategoryJpaEntity> {

    List<SubCategoryJpaEntity> findAllByMasterCategoryIdAndTenantId(
            UUID masterCategoryId, UUID tenantId);

    Optional<SubCategoryJpaEntity> findByMasterCategoryIdAndCode(UUID masterCategoryId, String code);
}
