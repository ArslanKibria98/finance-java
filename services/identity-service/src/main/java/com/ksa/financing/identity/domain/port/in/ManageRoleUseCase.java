package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Role;

import java.util.UUID;

public interface ManageRoleUseCase {

    record CreateRoleCommand(UUID tenantId, String roleCode, String roleName, String roleNameAr, String description) {}
    record UpdateRoleCommand(String roleName, String roleNameAr, String description, Boolean active) {}

    Role create(CreateRoleCommand command);
    Role getById(UUID tenantId, UUID roleId);
    Role getByCode(UUID tenantId, String roleCode);
    PageResponse<Role> listByTenant(UUID tenantId, PageQuery query);
    Role update(UUID tenantId, UUID roleId, UpdateRoleCommand command);
    void delete(UUID tenantId, UUID roleId);
}
