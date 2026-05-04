package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.adapter.rest.request.CreatePermissionRequest;
import com.ksa.financing.identity.adapter.rest.request.SyncRolePermissionsRequest;
import com.ksa.financing.identity.adapter.rest.request.UpdatePermissionRequest;
import com.ksa.financing.identity.adapter.rest.response.ModulePermissionsResponse;
import com.ksa.financing.identity.adapter.rest.response.PermissionResponse;
import com.ksa.financing.identity.domain.model.Module;
import com.ksa.financing.identity.domain.model.Permission;
import com.ksa.financing.identity.domain.port.in.ManagePermissionUseCase;
import com.ksa.financing.identity.domain.port.in.ManagePermissionUseCase.CreatePermissionCommand;
import com.ksa.financing.identity.domain.port.in.ManagePermissionUseCase.UpdatePermissionCommand;
import com.ksa.financing.identity.domain.port.out.ModuleRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Permissions", description = "Permission catalog management")
public class PermissionController {

    private final ManagePermissionUseCase managePermissionUseCase;
    private final ModuleRepository moduleRepository;

    @SecuredEndpoint(obj = "permissions", act = "create")
    @PostMapping
    @Operation(summary = "Create permission", description = "Creates a new permission")
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Creating permission: code={} tenant={}", request.permissionCode(), tenantId);

        var command = new CreatePermissionCommand(
                tenantId, request.permissionCode(), request.permissionName(),
                request.description(), request.resourceType(), request.action());

        var perm = managePermissionUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(perm));
    }

    @SecuredEndpoint(obj = "permissions", act = "read")
    @GetMapping
    @Operation(summary = "List permissions", description = "Returns catalog-visible permissions grouped by module")
    public ResponseEntity<List<ModulePermissionsResponse>> listPermissions(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var pageQuery = com.ksa.financing.infra.pagination.PageQuery.defaults(
                1000, "permissionCode", com.ksa.financing.infra.pagination.SortDirection.ASC);
        var perms = managePermissionUseCase.listByTenant(tenantId, pageQuery).content().stream()
                .filter(Permission::isCatalogVisible)
                .toList();
        var modules = moduleRepository.findCatalogByTenant(tenantId);
        return ResponseEntity.ok(groupByModule(perms, modules));
    }

    @SecuredEndpoint(obj = "permissions", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get permission", description = "Returns a single permission by ID")
    public ResponseEntity<PermissionResponse> getPermission(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var perm = managePermissionUseCase.getById(tenantId, id);
        return ResponseEntity.ok(toResponse(perm));
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "List permissions by role", description = "Returns permissions assigned to a role, grouped by module")
    public ResponseEntity<List<ModulePermissionsResponse>> listByRole(
            @PathVariable UUID roleId,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var perms = managePermissionUseCase.listByRole(tenantId, roleId);
        var modules = moduleRepository.findAllByTenant(tenantId);
        return ResponseEntity.ok(groupByModule(perms, modules));
    }

    @SecuredEndpoint(obj = "permissions", act = "create")
    @PutMapping("/{id}")
    @Operation(summary = "Update permission", description = "Updates permission information")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Updating permission: id={} tenant={}", id, tenantId);

        var command = new UpdatePermissionCommand(
                request.permissionName(), request.description(), request.active());

        var perm = managePermissionUseCase.update(tenantId, id, command);
        return ResponseEntity.ok(toResponse(perm));
    }

    @SecuredEndpoint(obj = "permissions", act = "create")
    @PutMapping("/role/{roleId}/sync")
    @Operation(summary = "Sync role permissions", description = "Replaces all permissions for a role with the provided list")
    public ResponseEntity<Void> syncRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody SyncRolePermissionsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Syncing {} permissions to role {} in tenant {}", request.permissionIds().size(), roleId, tenantId);
        managePermissionUseCase.syncRolePermissions(tenantId, roleId, request.permissionIds());
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "permissions", act = "create")
    @PostMapping("/role/{roleId}/assign/{permissionId}")
    @Operation(summary = "Assign permission to role", description = "Assigns a single permission to a role")
    public ResponseEntity<Void> assignToRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Assigning permission {} to role {} in tenant {}", permissionId, roleId, tenantId);
        managePermissionUseCase.assignToRole(tenantId, roleId, permissionId);
        return ResponseEntity.ok().build();
    }

    @SecuredEndpoint(obj = "permissions", act = "delete")
    @DeleteMapping("/role/{roleId}/remove/{permissionId}")
    @Operation(summary = "Remove permission from role", description = "Removes a permission from a role")
    public ResponseEntity<Void> removeFromRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal Jwt jwt) {

        var tenantId = extractTenantId(jwt);
        log.info("Removing permission {} from role {} in tenant {}", permissionId, roleId, tenantId);
        managePermissionUseCase.removeFromRole(tenantId, roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    private List<ModulePermissionsResponse> groupByModule(List<Permission> perms, List<Module> modules) {
        // Build module lookup by ID
        Map<UUID, Module> moduleById = modules.stream()
                .collect(Collectors.toMap(Module::getId, m -> m, (a, b) -> a));

        // Build module lookup by code (fallback for permissions without module_id)
        Map<String, Module> moduleByCode = modules.stream()
                .collect(Collectors.toMap(Module::getModuleCode, m -> m, (a, b) -> a));

        // Group permissions by module ID
        Map<UUID, List<PermissionResponse>> grouped = new LinkedHashMap<>();

        for (var perm : perms) {
            Module module = null;
            if (perm.getModuleId() != null) {
                module = moduleById.get(perm.getModuleId());
            }
            if (module == null && perm.getResourceType() != null) {
                module = moduleByCode.get(perm.getResourceType());
            }

            if (module != null) {
                grouped.computeIfAbsent(module.getId(), k -> new ArrayList<>())
                        .add(toResponse(perm));
            }
        }

        // Build response ordered by module displayOrder
        return modules.stream()
                .filter(m -> grouped.containsKey(m.getId()))
                .map(m -> new ModulePermissionsResponse(
                        m.getId(),
                        m.getModuleCode(),
                        m.getModuleName(),
                        m.getDescription(),
                        m.getDisplayOrder(),
                        grouped.get(m.getId())))
                .toList();
    }

    private PermissionResponse toResponse(Permission p) {
        return new PermissionResponse(
                p.getId(), p.getTenantId(), p.getPermissionCode(),
                p.getPermissionName(), p.getDescription(),
                p.getResourceType(), p.getModuleId(), p.getAction(), p.isActive(),
                p.getCreatedAt(), p.getUpdatedAt());
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
