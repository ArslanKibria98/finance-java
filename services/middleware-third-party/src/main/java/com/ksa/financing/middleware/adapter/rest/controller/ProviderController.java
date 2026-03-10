package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.CreateProviderRequest;
import com.ksa.financing.middleware.application.dto.ProviderResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderRequest;
import com.ksa.financing.middleware.domain.port.in.ManageProviderUseCase;
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
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ManageProviderUseCase manageProviderUseCase;

    @SecuredEndpoint(obj = "middleware.providers", act = "manage")
    @PostMapping
    public ResponseEntity<ProviderResponse> create(@Valid @RequestBody CreateProviderRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var createdBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageProviderUseCase.create(tenantId, request, createdBy));
    }

    @SecuredEndpoint(obj = "middleware.providers", act = "read")
    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getById(@PathVariable UUID id,
                                                     @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.providers", act = "read")
    @GetMapping("/code/{code}")
    public ResponseEntity<ProviderResponse> getByCode(@PathVariable String code,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderUseCase.getByCode(tenantId, code));
    }

    @SecuredEndpoint(obj = "middleware.providers", act = "read")
    @GetMapping
    public ResponseEntity<List<ProviderResponse>> listAll(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderUseCase.listAll(tenantId));
    }

    @SecuredEndpoint(obj = "middleware.providers", act = "manage")
    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateProviderRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderUseCase.update(tenantId, id, request));
    }

    @SecuredEndpoint(obj = "middleware.providers", act = "manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageProviderUseCase.delete(tenantId, id);
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
