package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.CreateProviderApiRequest;
import com.ksa.financing.middleware.application.dto.ProviderApiResponse;
import com.ksa.financing.middleware.application.dto.UpdateProviderApiRequest;
import com.ksa.financing.middleware.domain.port.in.ManageProviderApiUseCase;
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
@RequestMapping("/api/v1/provider-apis")
@RequiredArgsConstructor
public class ProviderApiController {

    private final ManageProviderApiUseCase manageProviderApiUseCase;

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @PostMapping
    public ResponseEntity<ProviderApiResponse> create(@Valid @RequestBody CreateProviderApiRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var createdBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageProviderApiUseCase.create(tenantId, request, createdBy));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "read")
    @GetMapping("/{id}")
    public ResponseEntity<ProviderApiResponse> getById(@PathVariable UUID id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "read")
    @GetMapping("/by-provider/{providerId}")
    public ResponseEntity<List<ProviderApiResponse>> listByProvider(@PathVariable UUID providerId,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.listByProvider(tenantId, providerId));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "read")
    @GetMapping
    public ResponseEntity<List<ProviderApiResponse>> listAll(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.listAll(tenantId));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @PutMapping("/{id}")
    public ResponseEntity<ProviderApiResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateProviderApiRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageProviderApiUseCase.update(tenantId, id, request));
    }

    @SecuredEndpoint(obj = "middleware.provider-apis", act = "manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageProviderApiUseCase.delete(tenantId, id);
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
