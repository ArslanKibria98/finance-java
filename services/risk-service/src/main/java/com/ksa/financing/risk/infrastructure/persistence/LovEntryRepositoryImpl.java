package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.port.out.LovEntryRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.LovEntryJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LovEntryRepositoryImpl implements LovEntryRepository {

    private static final Set<String> SEARCHABLE_FIELDS = Set.of("factorCode", "labelEn", "labelAr");
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("factorCode", "riskStatus", "active");

    private final JpaLovEntryRepository jpa;

    @Override
    public LovEntry save(LovEntry entry) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(entry)));
    }
    @Override
    public Optional<LovEntry> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public PageResponse<LovEntry> findByLovSetId(UUID tenantId, UUID lovSetId, PageQuery pageQuery) {
        var page = jpa.findAll(buildSpec(tenantId, lovSetId, false, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }
    @Override
    public PageResponse<LovEntry> findActiveByLovSetId(UUID tenantId, UUID lovSetId, PageQuery pageQuery) {
        var page = jpa.findAll(buildSpec(tenantId, lovSetId, true, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }

    private Specification<LovEntryJpaEntity> buildSpec(UUID tenantId, UUID lovSetId, boolean activeOnly, PageQuery pageQuery) {
        Specification<LovEntryJpaEntity> base = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("lovSetId"), lovSetId)
        );
        if (activeOnly) {
            base = base.and((root, q, cb) -> cb.isTrue(root.get("active")));
        }
        Specification<LovEntryJpaEntity> dynamic = SpecificationBuilder.<LovEntryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();
        return base.and(dynamic);
    }
    @Override
    public Optional<LovEntry> findByFactorCode(UUID tenantId, UUID lovSetId, String factorCode) {
        return jpa.findByTenantIdAndLovSetIdAndFactorCode(tenantId, lovSetId, factorCode).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public boolean existsByFactorCode(UUID tenantId, UUID lovSetId, String factorCode) {
        return jpa.existsByTenantIdAndLovSetIdAndFactorCode(tenantId, lovSetId, factorCode);
    }
}
