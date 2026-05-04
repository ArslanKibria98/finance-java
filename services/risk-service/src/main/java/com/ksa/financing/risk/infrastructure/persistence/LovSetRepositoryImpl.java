package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.risk.domain.model.lov.LovSet;
import com.ksa.financing.risk.domain.port.out.LovSetRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.LovSetJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LovSetRepositoryImpl implements LovSetRepository {

    private static final Set<String> SEARCHABLE_FIELDS = Set.of("code", "nameEn", "nameAr");
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("code", "categoryType", "active");

    private final JpaLovSetRepository jpa;

    @Override
    public LovSet save(LovSet lovSet) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(lovSet)));
    }
    @Override
    public Optional<LovSet> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public PageResponse<LovSet> findAllByTenantId(UUID tenantId, PageQuery pageQuery) {
        var page = jpa.findAll(buildSpec(tenantId, false, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }
    @Override
    public PageResponse<LovSet> findActiveByTenantId(UUID tenantId, PageQuery pageQuery) {
        var page = jpa.findAll(buildSpec(tenantId, true, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }
    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpa.existsByTenantIdAndCode(tenantId, code);
    }

    private Specification<LovSetJpaEntity> buildSpec(UUID tenantId, boolean activeOnly, PageQuery pageQuery) {
        Specification<LovSetJpaEntity> base = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (activeOnly) {
            base = base.and((root, q, cb) -> cb.isTrue(root.get("active")));
        }
        Specification<LovSetJpaEntity> dynamic = SpecificationBuilder.<LovSetJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();
        return base.and(dynamic);
    }
}
