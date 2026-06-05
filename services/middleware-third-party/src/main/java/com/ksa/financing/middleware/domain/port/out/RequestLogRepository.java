package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.domain.model.ApiRequestLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestLogRepository {
    ApiRequestLog save(ApiRequestLog log);
    Optional<ApiRequestLog> findById(UUID tenantId, UUID id);
    Optional<ApiRequestLog> findByRequestId(UUID tenantId, String requestId);
    Optional<ApiRequestLog> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    PageResponse<ApiRequestLog> findAll(UUID tenantId, PageQuery query);
    PageResponse<ApiRequestLog> findByApiIdIn(UUID tenantId, List<UUID> apiIds, PageQuery query);
}
