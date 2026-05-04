package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;
import com.ksa.financing.risk.domain.port.out.AuditEntryRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.AuditEntryJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditEntryRepositoryImpl implements AuditEntryRepository {
    private final JpaAuditEntryRepository jpa;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("entityType", "action", "status");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("entityId", "actorId", "correlationId", "details");

    @Override
    public AuditEntry save(AuditEntry entry) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(entry)));
    }

    @Override
    public PageResponse<AuditEntry> findByEntity(UUID tenantId, AuditEntityType entityType, UUID entityId, PageQuery query) {
        Specification<AuditEntryJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("entityType"), AuditEntryJpaEntity.AuditEntityTypeEnum.valueOf(entityType.name())),
                cb.equal(root.get("entityId"), entityId)
        );

        Specification<AuditEntryJpaEntity> dynamic = SpecificationBuilder.<AuditEntryJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<AuditEntryJpaEntity> page = jpa.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<AuditEntry> findByActor(UUID tenantId, UUID actorId, Instant from, Instant to, PageQuery query) {
        Specification<AuditEntryJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("actorId"), actorId),
                cb.between(root.get("createdAt"), from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC))
        );

        Specification<AuditEntryJpaEntity> dynamic = SpecificationBuilder.<AuditEntryJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<AuditEntryJpaEntity> page = jpa.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<AuditEntry> findByDateRange(UUID tenantId, Instant from, Instant to, PageQuery query) {
        Specification<AuditEntryJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.between(root.get("createdAt"), from.atOffset(ZoneOffset.UTC), to.atOffset(ZoneOffset.UTC))
        );

        Specification<AuditEntryJpaEntity> dynamic = SpecificationBuilder.<AuditEntryJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<AuditEntryJpaEntity> page = jpa.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<AuditEntry> findByCorrelationId(UUID tenantId, String correlationId, PageQuery query) {
        Specification<AuditEntryJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("correlationId"), correlationId)
        );

        Specification<AuditEntryJpaEntity> dynamic = SpecificationBuilder.<AuditEntryJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<AuditEntryJpaEntity> page = jpa.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<AuditEntry> findAllByTenant(UUID tenantId, PageQuery query) {
        Specification<AuditEntryJpaEntity> spec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);

        Specification<AuditEntryJpaEntity> dynamic = SpecificationBuilder.<AuditEntryJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<AuditEntryJpaEntity> page = jpa.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
    }
}
