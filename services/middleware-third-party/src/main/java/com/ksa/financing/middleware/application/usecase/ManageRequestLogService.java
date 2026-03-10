package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.RequestLogResponse;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.ApiRequestLog;
import com.ksa.financing.middleware.domain.port.in.ManageRequestLogUseCase;
import com.ksa.financing.middleware.domain.port.out.RequestLogRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ManageRequestLogService implements ManageRequestLogUseCase {

    private final RequestLogRepository requestLogRepository;
    private final MiddlewareMapper mapper;

    @Override
    @Transactional
    public ApiRequestLog create(ApiRequestLog requestLog) {
        return requestLogRepository.save(requestLog);
    }

    @Override
    public RequestLogResponse getById(UUID tenantId, UUID id) {
        return requestLogRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("RequestLog", id.toString()));
    }

    @Override
    public RequestLogResponse getByRequestId(UUID tenantId, String requestId) {
        return requestLogRepository.findByRequestId(tenantId, requestId)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("RequestLog", requestId));
    }

    @Override
    public List<RequestLogResponse> listAll(UUID tenantId, int page, int size) {
        return requestLogRepository.findAll(tenantId, page, size)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<RequestLogResponse> listByProvider(UUID tenantId, List<UUID> apiIds, int page, int size) {
        return requestLogRepository.findByApiIdIn(tenantId, apiIds, page, size)
                .stream().map(mapper::toResponse).toList();
    }
}
