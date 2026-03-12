package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.BulkGrantAccessRequest;
import com.ksa.financing.middleware.application.dto.BulkGrantAccessResponse;
import com.ksa.financing.middleware.application.dto.ClientApiAccessResponse;
import com.ksa.financing.middleware.application.dto.ClientProviderAccessResponse;
import com.ksa.financing.middleware.application.dto.GrantAccessRequest;
import com.ksa.financing.middleware.domain.port.in.ManageClientAccessUseCase;
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
@RequestMapping("/api/v1/clients/{clientId}/access")
@RequiredArgsConstructor
public class ClientAccessController {

    private final ManageClientAccessUseCase manageClientAccessUseCase;

    // ==================== Provider Access ====================

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @PostMapping("/providers")
    public ResponseEntity<ClientProviderAccessResponse> grantProviderAccess(
            @PathVariable UUID clientId,
            @Valid @RequestBody GrantAccessRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var grantedBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageClientAccessUseCase.grantProviderAccess(tenantId, clientId, request, grantedBy));
    }

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @GetMapping("/providers")
    public ResponseEntity<List<ClientProviderAccessResponse>> listProviderAccess(
            @PathVariable UUID clientId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientAccessUseCase.listProviderAccess(tenantId, clientId));
    }

    // ==================== Bulk Access (Providers + APIs in one call) ====================

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @PostMapping("/bulk")
    public ResponseEntity<BulkGrantAccessResponse> bulkGrantAccess(
            @PathVariable UUID clientId,
            @Valid @RequestBody BulkGrantAccessRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var grantedBy = UUID.fromString(jwt.getSubject());

        // Override clientId from path into request (ensure consistency)
        var effectiveRequest = new BulkGrantAccessRequest(clientId, request.environment(), request.providers());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageClientAccessUseCase.bulkGrantAccess(tenantId, effectiveRequest, grantedBy));
    }

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @DeleteMapping("/providers/{providerId}")
    public ResponseEntity<Void> revokeProviderAccess(
            @PathVariable UUID clientId,
            @PathVariable UUID providerId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageClientAccessUseCase.revokeProviderAccess(tenantId, clientId, providerId);
        return ResponseEntity.noContent().build();
    }

    // ==================== API Access ====================

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @PostMapping("/apis")
    public ResponseEntity<ClientApiAccessResponse> grantApiAccess(
            @PathVariable UUID clientId,
            @Valid @RequestBody GrantAccessRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var grantedBy = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageClientAccessUseCase.grantApiAccess(tenantId, clientId, request, grantedBy));
    }

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @GetMapping("/apis")
    public ResponseEntity<List<ClientApiAccessResponse>> listApiAccess(
            @PathVariable UUID clientId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientAccessUseCase.listApiAccess(tenantId, clientId));
    }

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @GetMapping("/apis/by-provider/{providerId}")
    public ResponseEntity<List<ClientApiAccessResponse>> listApiAccessByProvider(
            @PathVariable UUID clientId,
            @PathVariable UUID providerId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageClientAccessUseCase.listApiAccessByProvider(tenantId, clientId, providerId));
    }

    @SecuredEndpoint(obj = "middleware.client-access", act = "manage")
    @DeleteMapping("/apis/{apiId}")
    public ResponseEntity<Void> revokeApiAccess(
            @PathVariable UUID clientId,
            @PathVariable UUID apiId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        manageClientAccessUseCase.revokeApiAccess(tenantId, clientId, apiId);
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
