package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAssessmentSessionRepository extends JpaRepository<AssessmentSessionJpaEntity, UUID> {
    Optional<AssessmentSessionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AssessmentSessionJpaEntity> findAllByTenantIdAndEntityReference(UUID tenantId, String entityReference);
    Optional<AssessmentSessionJpaEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    Optional<AssessmentSessionJpaEntity> findFirstByTenantIdAndEntityReferenceOrderByCreatedAtDesc(UUID tenantId, String entityReference);
}
