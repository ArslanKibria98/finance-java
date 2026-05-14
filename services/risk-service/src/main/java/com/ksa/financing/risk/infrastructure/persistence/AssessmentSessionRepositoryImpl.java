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
        String search = pageQuery.search();
        org.springframework.data.jpa.domain.Specification<com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentSessionJpaEntity> baseSpec =
                (root, q, cb) -> {
                    var entRefPred = cb.equal(root.get("entityReference"), entityReference);
                    return tenantId == null ? entRefPred : cb.and(cb.equal(root.get("tenantId"), tenantId), entRefPred);
                };
        org.springframework.data.jpa.domain.Specification<com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentSessionJpaEntity> dynamic =
                com.ksa.financing.infra.pagination.SpecificationBuilder
                        .<com.ksa.financing.risk.infrastructure.persistence.entity.AssessmentSessionJpaEntity>builder()
                        .filters(pageQuery.filters())
                        .search(search)
                        .searchableFields(java.util.Set.of("entityReference", "status", "riskLevel"))
                        .build();
        var page = jpa.findAll(baseSpec.and(dynamic), pageable);
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
