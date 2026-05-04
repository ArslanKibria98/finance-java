package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;
import com.ksa.financing.risk.domain.port.out.AssessmentSessionRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public PageResponse<AssessmentSession> findByEntityReference(UUID tenantId, String entityReference, PageQuery pageQuery) {
        var pageable = pageQuery.toPageable();
        if (tenantId == null) {
            var page = jpa.findAllByEntityReference(entityReference, pageable);
            return PageResponse.from(page, RiskPersistenceMapper::toDomain);
        }
        var page = jpa.findAllByTenantIdAndEntityReference(tenantId, entityReference, pageable);
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
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
