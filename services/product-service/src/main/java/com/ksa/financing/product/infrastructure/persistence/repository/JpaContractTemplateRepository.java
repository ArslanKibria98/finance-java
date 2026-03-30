package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ContractTemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaContractTemplateRepository extends JpaRepository<ContractTemplateJpaEntity, UUID> {

    List<ContractTemplateJpaEntity> findAllByTenantIdAndIsActiveTrue(UUID tenantId);

    List<ContractTemplateJpaEntity> findAllByTenantIdAndProductIdAndIsActiveTrue(UUID tenantId, UUID productId);

    List<ContractTemplateJpaEntity> findAllByTenantIdAndTypeIdAndIsActiveTrue(UUID tenantId, UUID typeId);
}
