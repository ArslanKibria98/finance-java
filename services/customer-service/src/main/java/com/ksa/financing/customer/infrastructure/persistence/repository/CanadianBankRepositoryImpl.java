package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.CanadianBankOption;
import com.ksa.financing.customer.domain.port.out.CanadianBankRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.CanadianBankOptionJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.ReferenceDataPersistenceMapper;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CanadianBankRepositoryImpl implements CanadianBankRepository {

    private static final Set<String> SEARCHABLE_FIELDS = Set.of("code", "nameEn", "nameAr");
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("code", "active", "displayOrder");

    private final JpaCanadianBankRepository jpaRepository;

    @Override
    public Optional<CanadianBankOption> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<CanadianBankOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery) {
        log.debug("Finding all Canadian bank options for tenant: {} with query: {}", tenantId, pageQuery);
        var page = jpaRepository.findAll(buildSpec(tenantId, false, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<CanadianBankOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery) {
        log.debug("Finding active Canadian bank options for tenant: {} with query: {}", tenantId, pageQuery);
        var page = jpaRepository.findAll(buildSpec(tenantId, true, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, ReferenceDataPersistenceMapper::toDomain);
    }

    private Specification<CanadianBankOptionJpaEntity> buildSpec(UUID tenantId, boolean activeOnly, PageQuery pageQuery) {
        Specification<CanadianBankOptionJpaEntity> base = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.isFalse(root.get("deleted"))
        );
        if (activeOnly) {
            base = base.and((root, q, cb) -> cb.isTrue(root.get("active")));
        }
        Specification<CanadianBankOptionJpaEntity> dynamic = SpecificationBuilder.<CanadianBankOptionJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();
        return base.and(dynamic);
    }
}
