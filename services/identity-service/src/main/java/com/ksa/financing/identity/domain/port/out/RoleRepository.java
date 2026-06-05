package com.ksa.financing.identity.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.identity.domain.model.Role;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(UUID tenantId, UUID roleId);
    Optional<Role> findByCode(UUID tenantId, String roleCode);
    PageResponse<Role> findAllByTenant(UUID tenantId, PageQuery query);
    boolean existsByCode(UUID tenantId, String roleCode);
    void deleteById(UUID tenantId, UUID roleId);
}
