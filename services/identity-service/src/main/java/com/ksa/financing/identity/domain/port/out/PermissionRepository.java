package com.ksa.financing.identity.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findById(UUID tenantId, UUID permissionId);
    PageResponse<Permission> findAllByTenant(UUID tenantId, PageQuery query);
    List<Permission> findByRoleId(UUID tenantId, UUID roleId);
    boolean existsByCode(UUID tenantId, String permissionCode);
    void assignToRole(UUID tenantId, UUID roleId, UUID permissionId);
    void removeFromRole(UUID tenantId, UUID roleId, UUID permissionId);
    void removeAllFromRole(UUID roleId);
}
