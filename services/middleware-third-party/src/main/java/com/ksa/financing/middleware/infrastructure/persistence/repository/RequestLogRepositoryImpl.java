package com.ksa.financing.middleware.infrastructure.persistence.repository;

import com.ksa.financing.middleware.domain.model.ApiRequestLog;
import com.ksa.financing.middleware.domain.port.out.RequestLogRepository;
import com.ksa.financing.middleware.infrastructure.persistence.mapper.MiddlewarePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RequestLogRepositoryImpl implements RequestLogRepository {

    private final JpaRequestLogRepository jpaRepository;
    private final MiddlewarePersistenceMapper mapper;

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
    public List<ApiRequestLog> findAll(UUID tenantId, int page, int size) {
        return jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ApiRequestLog> findByApiIdIn(UUID tenantId, List<UUID> apiIds, int page, int size) {
        return jpaRepository.findByApiIdInAndTenantIdOrderByCreatedAtDesc(apiIds, tenantId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }
}
