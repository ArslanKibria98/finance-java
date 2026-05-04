package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.adapter.rest.request.CreateRoleRequest;
import com.ksa.financing.identity.adapter.rest.request.UpdateRoleRequest;
import com.ksa.financing.identity.adapter.rest.response.RoleResponse;
import com.ksa.financing.identity.domain.model.Role;
import com.ksa.financing.identity.domain.port.in.ManageRoleUseCase;
import com.ksa.financing.identity.domain.port.in.ManageRoleUseCase.CreateRoleCommand;
import com.ksa.financing.identity.domain.port.in.ManageRoleUseCase.UpdateRoleCommand;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles", description = "RBAC role management")
public class RoleController {

    private final ManageRoleUseCase manageRoleUseCase;

    @SecuredEndpoint(obj = "roles", act = "create")
    @PostMapping
    @Operation(summary = "Create role", description = "Creates a new role definition")
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating role: code={} tenant={}", request.roleCode(), tenantId);

        var command = new CreateRoleCommand(
                tenantId, request.roleCode(), request.roleName(),
                request.roleNameAr(), request.description());

        var role = manageRoleUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(role));
    }

    @SecuredEndpoint(obj = "roles", act = "read")
    @GetMapping
    @Operation(summary = "List roles", description = "Returns all roles for the tenant")
    public PageResponse<RoleResponse> listRoles(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var page = manageRoleUseCase.listByTenant(tenantId, query);
        return page.map(this::toResponse);
    }

    @SecuredEndpoint(obj = "roles", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get role", description = "Returns a single role by ID")
    public ResponseEntity<RoleResponse> getRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var role = manageRoleUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(role));
    }

    @SecuredEndpoint(obj = "roles", act = "read")
    @GetMapping("/code/{roleCode}")
    @Operation(summary = "Get role by code", description = "Returns a single role by role code")
    public ResponseEntity<RoleResponse> getRoleByCode(
            @PathVariable String roleCode,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var role = manageRoleUseCase.getByCode(tenantId, roleCode);
        return ResponseEntity.ok(toResponse(role));
    }

    @SecuredEndpoint(obj = "roles", act = "create")
    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Updates role information (system roles are immutable)")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating role: id={} tenant={}", id, tenantId);

        var command = new UpdateRoleCommand(
                request.roleName(), request.roleNameAr(),
                request.description(), request.active());

        var role = manageRoleUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(role));
    }

    @SecuredEndpoint(obj = "roles", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Deletes a non-system role")
    public ResponseEntity<Void> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        log.info("Deleting role: id={} tenant={}", id, tenantId);
        manageRoleUseCase.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private RoleResponse toResponse(Role r) {
        return new RoleResponse(
                r.getId(), r.getTenantId(), r.getRoleCode(),
                r.getRoleName(), r.getRoleNameAr(), r.getDescription(),
                r.isActive(), r.isSystem(),
                r.getCreatedAt(), r.getUpdatedAt(), r.getVersion());
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
