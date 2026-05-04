package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.application.dto.RequestLogResponse;
import com.ksa.financing.middleware.domain.port.in.ManageRequestLogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/request-logs")
@RequiredArgsConstructor
public class RequestLogController {

    private final ManageRequestLogUseCase manageRequestLogUseCase;

    @SecuredEndpoint(obj = "middleware.logs", act = "read")
    @GetMapping("/{id}")
    public ResponseEntity<RequestLogResponse> getById(@PathVariable UUID id,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageRequestLogUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.logs", act = "read")
    @GetMapping("/by-request-id/{requestId}")
    public ResponseEntity<RequestLogResponse> getByRequestId(@PathVariable String requestId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageRequestLogUseCase.getByRequestId(tenantId, requestId));
    }

    @SecuredEndpoint(obj = "middleware.logs", act = "read")
    @GetMapping
    public PageResponse<RequestLogResponse> listAll(PageQuery query,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return manageRequestLogUseCase.listAll(tenantId, query);
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaim("tenant_id");
        if (tenantClaim == null) {
            throw new com.ksa.financing.infra.exception.BusinessException(
                    "COMMON.AUTH.INVALID_CREDENTIALS",
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim.toString());
    }
}
