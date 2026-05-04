package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.application.dto.RequestLogResponse;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.ApiRequestLog;
import com.ksa.financing.middleware.domain.port.in.ManageRequestLogUseCase;
import com.ksa.financing.middleware.domain.port.out.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageRequestLogService implements ManageRequestLogUseCase {

    private final RequestLogRepository requestLogRepository;
    private final MiddlewareMapper mapper;

    @Override
    @Transactional
    public ApiRequestLog create(ApiRequestLog requestLog) {
        return requestLogRepository.save(requestLog);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestLogResponse getById(UUID tenantId, UUID id) {
        return requestLogRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Request log not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public RequestLogResponse getByRequestId(UUID tenantId, String requestId) {
        return requestLogRepository.findByRequestId(tenantId, requestId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Request log not found for request ID: " + requestId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequestLogResponse> listAll(UUID tenantId, PageQuery query) {
        return requestLogRepository.findAll(tenantId, query)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RequestLogResponse> listByProvider(UUID tenantId, List<UUID> apiIds, PageQuery query) {
        return requestLogRepository.findByApiIdIn(tenantId, apiIds, query)
                .map(mapper::toResponse);
    }
}
