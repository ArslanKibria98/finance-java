package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalActionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaApprovalActionRepository extends JpaRepository<ApprovalActionJpaEntity, UUID> {

    List<ApprovalActionJpaEntity> findByWorkflowIdOrderBySortOrder(UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}
