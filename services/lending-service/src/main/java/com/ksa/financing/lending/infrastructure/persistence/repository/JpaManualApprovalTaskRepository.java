package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.domain.model.ManualApprovalTask;
import com.ksa.financing.lending.infrastructure.persistence.entity.ManualApprovalTaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaManualApprovalTaskRepository extends JpaRepository<ManualApprovalTaskJpaEntity, UUID> {

    Optional<ManualApprovalTaskJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<ManualApprovalTaskJpaEntity> findByTenantIdAndApplicationId(UUID tenantId, UUID applicationId);

    List<ManualApprovalTaskJpaEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, ManualApprovalTask.Status status);

    List<ManualApprovalTaskJpaEntity> findByTenantIdAndAssignedRoleAndStatusOrderByCreatedAtDesc(
            UUID tenantId, String assignedRole, ManualApprovalTask.Status status);

    List<ManualApprovalTaskJpaEntity> findByStatusAndSlaDeadlineBefore(
            ManualApprovalTask.Status status, OffsetDateTime cutoff);
}
