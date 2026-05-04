package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;
import com.ksa.financing.lending.domain.port.out.PurposeOfFinanceRepository;
import com.ksa.financing.lending.infrastructure.persistence.entity.PurposeOfFinanceJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.mapper.PurposeOfFinancePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PurposeOfFinanceRepositoryImpl implements PurposeOfFinanceRepository {

    private final JpaPurposeOfFinanceRepository jpaRepository;
    private final PurposeOfFinancePersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("active", "code");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("nameEn", "nameAr", "code");

    @Override
    public PurposeOfFinanceEntry save(PurposeOfFinanceEntry entry) {
        var entity = mapper.toEntity(entry);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<PurposeOfFinanceEntry> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
    }

    @Override
    public Optional<PurposeOfFinanceEntry> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByTenantIdAndCode(tenantId, code).map(mapper::toDomain);
    }

    @Override
    public PageResponse<PurposeOfFinanceEntry> findAllActive(UUID tenantId, PageQuery query) {
        Specification<PurposeOfFinanceJpaEntity> spec = (root, q, cb) -> 
                cb.and(cb.equal(root.get("tenantId"), tenantId), cb.equal(root.get("active"), true));
        return findWithSpec(spec, query);
    }

    @Override
    public PageResponse<PurposeOfFinanceEntry> findAll(UUID tenantId, PageQuery query) {
        Specification<PurposeOfFinanceJpaEntity> spec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        return findWithSpec(spec, query);
    }

    private PageResponse<PurposeOfFinanceEntry> findWithSpec(Specification<PurposeOfFinanceJpaEntity> baseSpec, PageQuery query) {
        Specification<PurposeOfFinanceJpaEntity> dynamic = SpecificationBuilder.<PurposeOfFinanceJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<PurposeOfFinanceJpaEntity> page = jpaRepository.findAll(baseSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        jpaRepository.deleteByTenantIdAndId(tenantId, id);
    }
}
