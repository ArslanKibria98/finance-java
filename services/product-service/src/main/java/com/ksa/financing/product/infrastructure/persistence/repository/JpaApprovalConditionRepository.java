package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalConditionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaApprovalConditionRepository extends JpaRepository<ApprovalConditionJpaEntity, UUID> {

    List<ApprovalConditionJpaEntity> findByWorkflowIdOrderBySortOrder(UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}
