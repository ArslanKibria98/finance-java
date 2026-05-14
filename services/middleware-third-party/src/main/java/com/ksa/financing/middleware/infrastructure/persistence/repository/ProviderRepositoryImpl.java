package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.middleware.domain.model.ThirdPartyProvider;
import com.ksa.financing.middleware.domain.port.out.ProviderRepository;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ThirdPartyProviderJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.mapper.MiddlewarePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProviderRepositoryImpl implements ProviderRepository {

    private final JpaProviderRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("code", "active");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("code", "nameEn", "nameAr", "descriptionEn", "descriptionAr");

    @Override
    public ThirdPartyProvider save(ThirdPartyProvider provider) {
        log.debug("Saving provider: code={}, tenantId={}", provider.getCode(), provider.getTenantId());
        var entity = mapper.toEntity(provider);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ThirdPartyProvider> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ThirdPartyProvider> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResponse<ThirdPartyProvider> findAllByTenant(UUID tenantId, PageQuery query) {
        Specification<ThirdPartyProviderJpaEntity> tenantSpec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.isNull(root.get("deletedAt"))
        );

        Specification<ThirdPartyProviderJpaEntity> dynamic = SpecificationBuilder.<ThirdPartyProviderJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<ThirdPartyProviderJpaEntity> page = jpaRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }

    @Override
    public void deleteById(UUID tenantId, UUID id) {
        jpaRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .ifPresent(jpaRepository::delete);
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpaRepository.existsByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId);
    }
}
