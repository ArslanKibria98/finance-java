package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.CallbackResponseDto;
import com.ksa.financing.middleware.domain.model.CallbackResponse;

import java.util.List;
import java.util.UUID;

public interface ManageCallbackUseCase {
    CallbackResponse create(CallbackResponse callback);
    CallbackResponseDto getById(UUID tenantId, UUID id);
    List<CallbackResponseDto> listAll(UUID tenantId, int page, int size);
}
