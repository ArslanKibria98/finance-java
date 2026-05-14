package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAssessmentSessionRepository extends JpaRepository<AssessmentSessionJpaEntity, UUID>,
        JpaSpecificationExecutor<AssessmentSessionJpaEntity> {
    Optional<AssessmentSessionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<AssessmentSessionJpaEntity> findAllByTenantIdAndEntityReference(UUID tenantId, String entityReference, Pageable pageable);
    Page<AssessmentSessionJpaEntity> findAllByEntityReference(String entityReference, Pageable pageable);
    Optional<AssessmentSessionJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    Optional<AssessmentSessionJpaEntity> findFirstByTenantIdAndEntityReferenceOrderByCreatedAtDesc(UUID tenantId, String entityReference);
}
