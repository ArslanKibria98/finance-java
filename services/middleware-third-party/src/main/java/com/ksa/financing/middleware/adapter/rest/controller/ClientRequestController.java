package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.application.dto.ClientRequestResponse;
import com.ksa.financing.middleware.domain.model.EnvironmentType;
import com.ksa.financing.middleware.domain.port.in.ManageClientRequestUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only display API for per-environment third-party request logs.
 *
 * Paths are environment-segmented so the admin console can hit a clean URL
 * per env and the access policy can grant TEST visibility separately from PROD.
 *   GET /api/v1/client-requests/test
 *   GET /api/v1/client-requests/dev
 *   GET /api/v1/client-requests/prod
 */
@RestController
@RequestMapping("/api/v1/client-requests")
@RequiredArgsConstructor
@Tag(name = "Client Requests", description = "Per-environment third-party API call audit logs")
public class ClientRequestController {

    private final ManageClientRequestUseCase manageClientRequestUseCase;

    @Operation(summary = "List client requests for an environment (paginated)")
    @SecuredEndpoint(obj = "middleware.client-requests", act = "read")
    @GetMapping("/{environment}")
    public PageResponse<ClientRequestResponse> list(@PathVariable String environment,
                                                     PageQuery query,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var env = parseEnvironment(environment);
        var tenantId = extractTenantId(jwt);
        return manageClientRequestUseCase.listAll(tenantId, env, query);
    }

    @Operation(summary = "Fetch a single client request by UUID within an environment")
    @SecuredEndpoint(obj = "middleware.client-requests", act = "read")
    @GetMapping("/{environment}/{id}")
    public ResponseEntity<ClientRequestResponse> getById(@PathVariable String environment,
                                                          @PathVariable UUID id,
                                                          @AuthenticationPrincipal Jwt jwt) {
        var env = parseEnvironment(environment);
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientRequestUseCase.getById(tenantId, env, id));
    }

    @Operation(summary = "Fetch a single client request by its request_id (Req-NNNNNNNNNN)")
    @SecuredEndpoint(obj = "middleware.client-requests", act = "read")
    @GetMapping("/{environment}/by-request-id/{requestId}")
    public ResponseEntity<ClientRequestResponse> getByRequestId(@PathVariable String environment,
                                                                 @PathVariable String requestId,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        var env = parseEnvironment(environment);
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientRequestUseCase.getByRequestId(tenantId, env, requestId));
    }

    private EnvironmentType parseEnvironment(String raw) {
        try {
            return EnvironmentType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "Invalid environment: " + raw + ". Allowed values: TEST, DEV, PROD");
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaim("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim.toString());
    }
}
