package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.application.dto.CreateProviderRequest;
import com.ksa.financing.middleware.application.dto.ProviderEnvironmentResponse;
import com.ksa.financing.middleware.application.dto.ProviderResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderRequest;

import java.util.UUID;

public interface ManageProviderUseCase {
    ProviderResponse create(UUID tenantId, CreateProviderRequest request, UUID createdBy);
    ProviderResponse getById(UUID tenantId, UUID id);
    ProviderResponse getByCode(UUID tenantId, String code);
    PageResponse<ProviderResponse> listAll(UUID tenantId, PageQuery query);
    PageResponse<ProviderEnvironmentResponse> listAllWithEnvironment(UUID tenantId, PageQuery query);
    ProviderEnvironmentResponse getByIdWithEnvironment(UUID tenantId, UUID id);
    ProviderResponse update(UUID tenantId, UUID id, UpdateProviderRequest request);
    void delete(UUID tenantId, UUID id);
}
