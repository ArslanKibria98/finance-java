package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.identity.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface ManageRoleUseCase {

    record CreateRoleCommand(UUID tenantId, String roleCode, String roleName, String roleNameAr, String description) {}
    record UpdateRoleCommand(String roleName, String roleNameAr, String description, Boolean active) {}

    Role create(CreateRoleCommand command);
    Role getById(UUID tenantId, UUID roleId);
    Role getByCode(UUID tenantId, String roleCode);
    List<Role> listByTenant(UUID tenantId);
    Role update(UUID tenantId, UUID roleId, UpdateRoleCommand command);
    void delete(UUID tenantId, UUID roleId);
}
