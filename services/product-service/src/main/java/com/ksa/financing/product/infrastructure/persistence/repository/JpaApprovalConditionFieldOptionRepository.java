package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalConditionFieldOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaApprovalConditionFieldOptionRepository extends JpaRepository<ApprovalConditionFieldOptionJpaEntity, UUID> {

    List<ApprovalConditionFieldOptionJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrder(UUID tenantId);
}
