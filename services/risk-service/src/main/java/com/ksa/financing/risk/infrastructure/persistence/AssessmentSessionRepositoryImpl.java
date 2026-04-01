package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;
import com.ksa.financing.risk.domain.port.out.AssessmentSessionRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AssessmentSessionRepositoryImpl implements AssessmentSessionRepository {
    private final JpaAssessmentSessionRepository jpa;

    @Override
    public AssessmentSession save(AssessmentSession session) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(session)));
    }
    @Override
    public Optional<AssessmentSession> findById(UUID tenantId, UUID id) {
        if (tenantId == null) {
            return jpa.findById(id).map(RiskPersistenceMapper::toDomain);
        }
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<AssessmentSession> findByEntityReference(UUID tenantId, String entityReference) {
        if (tenantId == null) {
            return jpa.findAllByEntityReference(entityReference).stream().map(RiskPersistenceMapper::toDomain).toList();
        }
        return jpa.findAllByTenantIdAndEntityReference(tenantId, entityReference).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public Optional<AssessmentSession> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpa.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public Optional<AssessmentSession> findLatestByEntityReference(UUID tenantId, String entityReference) {
        return jpa.findFirstByTenantIdAndEntityReferenceOrderByCreatedAtDesc(tenantId, entityReference).map(RiskPersistenceMapper::toDomain);
    }
}
