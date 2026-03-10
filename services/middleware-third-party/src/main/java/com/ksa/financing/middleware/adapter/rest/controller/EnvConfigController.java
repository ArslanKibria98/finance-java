package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.CreateEnvConfigRequest;
import com.ksa.financing.middleware.application.dto.EnvConfigResponse;
import com.ksa.financing.middleware.domain.port.in.ManageEnvConfigUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/env-configs")
@RequiredArgsConstructor
public class EnvConfigController {

    private final ManageEnvConfigUseCase manageEnvConfigUseCase;

    @SecuredEndpoint(obj = "middleware.env-configs", act = "manage")
    @PostMapping
    public ResponseEntity<EnvConfigResponse> create(@Valid @RequestBody CreateEnvConfigRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var createdBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageEnvConfigUseCase.create(tenantId, request, createdBy));
    }

    @SecuredEndpoint(obj = "middleware.env-configs", act = "manage")
    @GetMapping("/{id}")
    public ResponseEntity<EnvConfigResponse> getById(@PathVariable UUID id,
                                                      @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageEnvConfigUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.env-configs", act = "manage")
    @GetMapping("/by-api/{apiId}")
    public ResponseEntity<List<EnvConfigResponse>> listByApi(@PathVariable UUID apiId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageEnvConfigUseCase.listByApi(tenantId, apiId));
    }

    @SecuredEndpoint(obj = "middleware.env-configs", act = "manage")
    @PutMapping("/{id}")
    public ResponseEntity<EnvConfigResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody CreateEnvConfigRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageEnvConfigUseCase.update(tenantId, id, request));
    }

    @SecuredEndpoint(obj = "middleware.env-configs", act = "manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageEnvConfigUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
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
