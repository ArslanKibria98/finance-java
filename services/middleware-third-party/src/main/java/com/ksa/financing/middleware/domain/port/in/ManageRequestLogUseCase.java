package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.RequestLogResponse;
import com.ksa.financing.middleware.domain.model.ApiRequestLog;

import java.util.List;
import java.util.UUID;

public interface ManageRequestLogUseCase {
    ApiRequestLog create(ApiRequestLog log);
    RequestLogResponse getById(UUID tenantId, UUID id);
    RequestLogResponse getByRequestId(UUID tenantId, String requestId);
    List<RequestLogResponse> listAll(UUID tenantId, int page, int size);
    List<RequestLogResponse> listByProvider(UUID tenantId, List<UUID> apiIds, int page, int size);
}
