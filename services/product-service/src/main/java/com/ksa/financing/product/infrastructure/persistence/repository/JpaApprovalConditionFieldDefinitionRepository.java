package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalConditionFieldDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaApprovalConditionFieldDefinitionRepository extends JpaRepository<ApprovalConditionFieldDefinitionJpaEntity, UUID>, JpaSpecificationExecutor<ApprovalConditionFieldDefinitionJpaEntity> {

    List<ApprovalConditionFieldDefinitionJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);
}
