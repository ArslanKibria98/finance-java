package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.middleware.domain.model.ApiRequestLog;
import com.ksa.financing.middleware.domain.port.out.RequestLogRepository;
import com.ksa.financing.middleware.infrastructure.persistence.entity.ApiRequestLogJpaEntity;
import com.ksa.financing.middleware.infrastructure.persistence.mapper.MiddlewarePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RequestLogRepositoryImpl implements RequestLogRepository {

    private final JpaRequestLogRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("apiId", "status", "providerId");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("requestId", "idempotencyKey", "requestUrl", "requestBody", "responseBody");

    @Override
    public ApiRequestLog save(ApiRequestLog requestLog) {
        log.debug("Saving request log: requestId={}, tenantId={}", requestLog.getRequestId(), requestLog.getTenantId());
        var entity = mapper.toEntity(requestLog);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ApiRequestLog> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByIdAndTenantId(id, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ApiRequestLog> findByRequestId(UUID tenantId, String requestId) {
        return jpaRepository.findByRequestIdAndTenantId(requestId, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ApiRequestLog> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepository.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResponse<ApiRequestLog> findAll(UUID tenantId, PageQuery query) {
        Specification<ApiRequestLogJpaEntity> tenantSpec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);

        Specification<ApiRequestLogJpaEntity> dynamic = SpecificationBuilder.<ApiRequestLogJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<ApiRequestLogJpaEntity> page = jpaRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }

    @Override
    public PageResponse<ApiRequestLog> findByApiIdIn(UUID tenantId, List<UUID> apiIds, PageQuery query) {
        Specification<ApiRequestLogJpaEntity> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                root.get("apiId").in(apiIds)
        );

        Specification<ApiRequestLogJpaEntity> dynamic = SpecificationBuilder.<ApiRequestLogJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<ApiRequestLogJpaEntity> page = jpaRepository.findAll(spec.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }
}
