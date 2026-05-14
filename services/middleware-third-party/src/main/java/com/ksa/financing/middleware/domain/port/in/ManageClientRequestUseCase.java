package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.application.dto.ClientRequestResponse;
import com.ksa.financing.middleware.domain.model.EnvironmentType;

import java.util.UUID;

/**
 * Display-side use case for the per-environment client_request tables.
 * Operators query these from the admin console to inspect third-party
 * call history grouped by environment.
 */
public interface ManageClientRequestUseCase {

    ClientRequestResponse getById(UUID tenantId, EnvironmentType environment, UUID id);

    ClientRequestResponse getByRequestId(UUID tenantId, EnvironmentType environment, String requestId);

    PageResponse<ClientRequestResponse> listAll(UUID tenantId, EnvironmentType environment, PageQuery query);
}
