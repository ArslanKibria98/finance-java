package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.ClientResponse;
import com.ksa.financing.middleware.application.dto.CreateClientRequest;
import com.ksa.financing.middleware.domain.port.in.ManageClientUseCase;
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
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ManageClientUseCase manageClientUseCase;

    @SecuredEndpoint(obj = "middleware.clients", act = "manage")
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var createdBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageClientUseCase.create(tenantId, request, createdBy));
    }

    @SecuredEndpoint(obj = "middleware.clients", act = "manage")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getById(@PathVariable UUID id,
                                                   @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.clients", act = "manage")
    @GetMapping
    public ResponseEntity<List<ClientResponse>> listAll(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientUseCase.listAll(tenantId));
    }

    @SecuredEndpoint(obj = "middleware.clients", act = "manage")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody CreateClientRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientUseCase.update(tenantId, id, request));
    }

    @SecuredEndpoint(obj = "middleware.clients", act = "manage")
    @PostMapping("/{id}/regenerate-secret")
    public ResponseEntity<ClientResponse> regenerateSecret(@PathVariable UUID id,
                                                            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientUseCase.regenerateSecret(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.clients", act = "manage")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageClientUseCase.delete(tenantId, id);
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
