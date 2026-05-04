package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Permission;

import java.util.List;
import java.util.UUID;

public interface ManagePermissionUseCase {

    record CreatePermissionCommand(UUID tenantId, String permissionCode, String permissionName,
                                   String description, String resourceType, String action) {}
    record UpdatePermissionCommand(String permissionName, String description, Boolean active) {}

    Permission create(CreatePermissionCommand command);
    Permission getById(UUID tenantId, UUID permissionId);
    PageResponse<Permission> listByTenant(UUID tenantId, PageQuery query);
    List<Permission> listByRole(UUID tenantId, UUID roleId);
    Permission update(UUID tenantId, UUID permissionId, UpdatePermissionCommand command);
    void assignToRole(UUID tenantId, UUID roleId, UUID permissionId);
    void removeFromRole(UUID tenantId, UUID roleId, UUID permissionId);
    void syncRolePermissions(UUID tenantId, UUID roleId, List<UUID> permissionIds);
}
