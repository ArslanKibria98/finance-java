package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.RelationshipOption;
import com.ksa.financing.customer.domain.port.out.RelationshipRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.RelationshipOptionJpaEntity;
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
public class RelationshipRepositoryImpl implements RelationshipRepository {

    private static final Set<String> SEARCHABLE_FIELDS = Set.of("code", "nameEn", "nameAr");
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("code", "active", "displayOrder");

    private final JpaRelationshipRepository jpaRepository;

    @Override
    public RelationshipOption save(RelationshipOption option) {
        var entity = ReferenceDataPersistenceMapper.toRelationshipEntity(option);
        var saved = jpaRepository.save(entity);
        return ReferenceDataPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<RelationshipOption> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .map(ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<RelationshipOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery) {
        log.debug("Finding all relationship options for tenant: {} with query: {}", tenantId, pageQuery);
        var page = jpaRepository.findAll(buildSpec(tenantId, false, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, ReferenceDataPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<RelationshipOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery) {
        log.debug("Finding active relationship options for tenant: {} with query: {}", tenantId, pageQuery);
        var page = jpaRepository.findAll(buildSpec(tenantId, true, pageQuery), pageQuery.toPageable());
        return PageResponse.from(page, ReferenceDataPersistenceMapper::toDomain);
    }

    private Specification<RelationshipOptionJpaEntity> buildSpec(UUID tenantId, boolean activeOnly, PageQuery pageQuery) {
        Specification<RelationshipOptionJpaEntity> base = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.isFalse(root.get("deleted"))
        );
        if (activeOnly) {
            base = base.and((root, q, cb) -> cb.isTrue(root.get("active")));
        }
        Specification<RelationshipOptionJpaEntity> dynamic = SpecificationBuilder.<RelationshipOptionJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();
        return base.and(dynamic);
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code) {
        return jpaRepository.existsByTenantIdAndCodeAndDeletedFalse(tenantId, code);
    }

    @Override
    public void softDelete(UUID tenantId, UUID id) {
        jpaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setActive(false);
            jpaRepository.save(entity);
        });
    }
}
