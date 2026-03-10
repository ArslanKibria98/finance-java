package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.CreateProviderApiRequest;
import com.ksa.financing.middleware.application.dto.ProviderApiResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderApiRequest;

import java.util.List;
import java.util.UUID;

public interface ManageProviderApiUseCase {
    ProviderApiResponse create(UUID tenantId, CreateProviderApiRequest request, UUID createdBy);
    ProviderApiResponse getById(UUID tenantId, UUID id);
    List<ProviderApiResponse> listByProvider(UUID tenantId, UUID providerId);
    List<ProviderApiResponse> listAll(UUID tenantId);
    ProviderApiResponse update(UUID tenantId, UUID id, UpdateProviderApiRequest request);
    void delete(UUID tenantId, UUID id);
}
