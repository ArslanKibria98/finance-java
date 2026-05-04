package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import com.ksa.financing.ledger.domain.model.CoaFieldStatus;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaFieldLovJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.mapper.CoaFieldLovPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CoaFieldLovRepositoryImpl implements CoaFieldLovRepository {

    private static final Set<String> SEARCHABLE_FIELDS = Set.of("fieldKey", "fieldLabelEn", "fieldLabelAr");
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("fieldKey", "category", "status", "mandatoryDefault");

    private final JpaCoaFieldLovRepository jpaRepository;
    private final CoaFieldLovPersistenceMapper mapper;

    @Override
    public CoaFieldLov save(CoaFieldLov fieldLov) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(fieldLov)));
    }

    @Override
    public Optional<CoaFieldLov> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
    }

    @Override
    public Optional<CoaFieldLov> findByFieldKey(UUID tenantId, String fieldKey) {
        return jpaRepository.findByTenantIdAndFieldKey(tenantId, fieldKey.trim().toUpperCase()).map(mapper::toDomain);
    }

    @Override
    public PageResponse<CoaFieldLov> findAllByTenant(UUID tenantId, PageQuery pageQuery) {
        var page = jpaRepository.findAll(buildSpec(tenantId, false, pageQuery), pageableWithDefaultSort(pageQuery));
        return PageResponse.from(page, mapper::toDomain);
    }

    @Override
    public PageResponse<CoaFieldLov> findAllActiveByTenant(UUID tenantId, PageQuery pageQuery) {
        var page = jpaRepository.findAll(buildSpec(tenantId, true, pageQuery), pageableWithDefaultSort(pageQuery));
        return PageResponse.from(page, mapper::toDomain);
    }

    private Specification<CoaFieldLovJpaEntity> buildSpec(UUID tenantId, boolean activeOnly, PageQuery pageQuery) {
        Specification<CoaFieldLovJpaEntity> base = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (activeOnly) {
            base = base.and((root, q, cb) -> cb.equal(root.get("status"), CoaFieldStatus.ACTIVE.name()));
        }
        Specification<CoaFieldLovJpaEntity> dynamic = SpecificationBuilder.<CoaFieldLovJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();
        return base.and(dynamic);
    }

    private org.springframework.data.domain.Pageable pageableWithDefaultSort(PageQuery pageQuery) {
        if (pageQuery.sort().isEmpty()) {
            return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by("displayOrder").ascending());
        }
        return pageQuery.toPageable();
    }

    @Override
    public boolean existsByFieldKey(UUID tenantId, String fieldKey) {
        return jpaRepository.existsByTenantIdAndFieldKey(tenantId, fieldKey.trim().toUpperCase());
    }
}
