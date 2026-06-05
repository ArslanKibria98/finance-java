package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.middleware.domain.model.CallbackResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallbackRepository {
    CallbackResponse save(CallbackResponse callback);
    Optional<CallbackResponse> findById(UUID tenantId, UUID id);
    List<CallbackResponse> findAll(UUID tenantId, int page, int size);
}
