package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.CreateEnvConfigRequest;
import com.ksa.financing.middleware.application.dto.EnvConfigResponse;

import java.util.List;
import java.util.UUID;

public interface ManageEnvConfigUseCase {
    EnvConfigResponse create(UUID tenantId, CreateEnvConfigRequest request, UUID createdBy);
    EnvConfigResponse getById(UUID tenantId, UUID id);
    List<EnvConfigResponse> listByApi(UUID tenantId, UUID apiId);
    EnvConfigResponse update(UUID tenantId, UUID id, CreateEnvConfigRequest request);
    void delete(UUID tenantId, UUID id);
}
