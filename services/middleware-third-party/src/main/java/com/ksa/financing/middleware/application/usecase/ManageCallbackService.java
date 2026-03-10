package com.ksa.financing.middleware.application.usecase;

import com.ksa.financing.middleware.application.dto.CallbackResponseDto;
import com.ksa.financing.middleware.application.mapper.MiddlewareMapper;
import com.ksa.financing.middleware.domain.model.CallbackResponse;
import com.ksa.financing.middleware.domain.port.in.ManageCallbackUseCase;
import com.ksa.financing.middleware.domain.port.out.CallbackRepository;
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
public class ManageCallbackService implements ManageCallbackUseCase {

    private final CallbackRepository callbackRepository;
    private final MiddlewareMapper mapper;

    @Override
    @Transactional
    public CallbackResponse create(CallbackResponse callback) {
        var saved = callbackRepository.save(callback);
        log.info("Saved callback: id={}, apiId={}", saved.getId(), saved.getApiId());
        return saved;
    }

    @Override
    public CallbackResponseDto getById(UUID tenantId, UUID id) {
        return callbackRepository.findById(tenantId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> NotFoundException.forEntity("Callback", id.toString()));
    }

    @Override
    public List<CallbackResponseDto> listAll(UUID tenantId, int page, int size) {
        return callbackRepository.findAll(tenantId, page, size)
                .stream().map(mapper::toResponse).toList();
    }
}
