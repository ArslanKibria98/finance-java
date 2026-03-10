package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.ClientResponse;
import com.ksa.financing.middleware.application.dto.CreateClientRequest;

import java.util.List;
import java.util.UUID;

public interface ManageClientUseCase {
    ClientResponse create(UUID tenantId, CreateClientRequest request, UUID createdBy);
    ClientResponse getById(UUID tenantId, UUID id);
    List<ClientResponse> listAll(UUID tenantId);
    ClientResponse update(UUID tenantId, UUID id, CreateClientRequest request);
    ClientResponse regenerateSecret(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);
}
