package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentAnswerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAssessmentAnswerRepository extends JpaRepository<AssessmentAnswerJpaEntity, UUID> {
    Optional<AssessmentAnswerJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AssessmentAnswerJpaEntity> findAllByTenantIdAndSessionId(UUID tenantId, UUID sessionId);
    List<AssessmentAnswerJpaEntity> findAllBySessionId(UUID sessionId);
    List<AssessmentAnswerJpaEntity> findAllByTenantIdAndSessionIdAndVersionStatus(UUID tenantId, UUID sessionId, AssessmentAnswerJpaEntity.VersionStatusEnum versionStatus);
    List<AssessmentAnswerJpaEntity> findAllBySessionIdAndVersionStatus(UUID sessionId, AssessmentAnswerJpaEntity.VersionStatusEnum versionStatus);
    List<AssessmentAnswerJpaEntity> findAllByTenantIdAndSessionIdAndParameterId(UUID tenantId, UUID sessionId, UUID parameterId);
}
