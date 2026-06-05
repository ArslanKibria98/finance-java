package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.domain.model.ClientRequest;
import com.ksa.financing.middleware.domain.model.EnvironmentType;

import java.util.Optional;
import java.util.UUID;

/**
 * Dispatching repository for per-environment client_request_{test|dev|prod} tables.
 * The environment of the persisted ClientRequest determines which physical
 * table is read/written.
 */
public interface ClientRequestRepository {

    ClientRequest save(ClientRequest request);

    Optional<ClientRequest> findById(UUID tenantId, EnvironmentType environment, UUID id);

    Optional<ClientRequest> findByRequestId(UUID tenantId, EnvironmentType environment, String requestId);

    Optional<ClientRequest> findByIdempotencyKey(UUID tenantId, EnvironmentType environment, String idempotencyKey);

    PageResponse<ClientRequest> findAll(UUID tenantId, EnvironmentType environment, PageQuery query);
}
