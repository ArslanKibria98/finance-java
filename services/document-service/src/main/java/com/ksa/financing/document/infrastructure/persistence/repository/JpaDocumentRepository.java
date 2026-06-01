package com.ksa.financing.document.infrastructure.persistence.repository;

import com.ksa.financing.document.infrastructure.persistence.entity.DocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    Optional<DocumentJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<DocumentJpaEntity> findByTenantIdAndIdempotencyKeyAndDeletedAtIsNull(
            UUID tenantId, String idempotencyKey);

    List<DocumentJpaEntity> findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID customerId);

    List<DocumentJpaEntity> findByWorkflowIdAndDeletedAtIsNullOrderByCreatedAtDesc(String workflowId);
}
