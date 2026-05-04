package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.TemplateTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTemplateTypeRepository extends JpaRepository<TemplateTypeJpaEntity, UUID>, JpaSpecificationExecutor<TemplateTypeJpaEntity> {

    List<TemplateTypeJpaEntity> findAllByTenantIdAndIsActiveTrue(UUID tenantId);

    List<TemplateTypeJpaEntity> findAllByTenantIdAndCategoryAndIsActiveTrue(UUID tenantId, String category);

    Optional<TemplateTypeJpaEntity> findByTenantIdAndNameEnAndCategory(UUID tenantId, String nameEn, String category);
}
