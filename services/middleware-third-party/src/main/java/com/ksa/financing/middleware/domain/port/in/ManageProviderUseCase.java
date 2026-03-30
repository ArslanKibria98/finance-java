package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.CreateProviderRequest;
import com.ksa.financing.middleware.application.dto.ProviderEnvironmentResponse;
import com.ksa.financing.middleware.application.dto.ProviderResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderRequest;

import java.util.List;
import java.util.UUID;

public interface ManageProviderUseCase {
    ProviderResponse create(UUID tenantId, CreateProviderRequest request, UUID createdBy);
    ProviderResponse getById(UUID tenantId, UUID id);
    ProviderResponse getByCode(UUID tenantId, String code);
    List<ProviderResponse> listAll(UUID tenantId);
    List<ProviderEnvironmentResponse> listAllWithEnvironment(UUID tenantId);
    ProviderEnvironmentResponse getByIdWithEnvironment(UUID tenantId, UUID id);
    ProviderResponse update(UUID tenantId, UUID id, UpdateProviderRequest request);
    void delete(UUID tenantId, UUID id);
}
