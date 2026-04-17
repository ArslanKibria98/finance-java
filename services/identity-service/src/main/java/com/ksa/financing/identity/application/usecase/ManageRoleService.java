package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.Role;
import com.ksa.financing.identity.domain.port.in.ManageRoleUseCase;
import com.ksa.financing.identity.domain.port.out.RoleRepository;
import com.ksa.financing.identity.infrastructure.persistence.repository.JpaRolePermissionRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageRoleService implements ManageRoleUseCase {

    private final RoleRepository roleRepository;
    private final JpaRolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public Role create(CreateRoleCommand command) {
        if (roleRepository.existsByCode(command.tenantId(), command.roleCode())) {
            throw new BusinessException(
                    ErrorCodes.Identity.ROLE_DUPLICATE,
                    "Role with code already exists: " + command.roleCode(),
                    command.roleCode());
        }

        var role = new Role();
        role.setTenantId(command.tenantId());
        role.setRoleCode(command.roleCode());
        role.setRoleName(command.roleName());
        role.setRoleNameAr(command.roleNameAr());
        role.setDescription(command.description());
        role.setActive(true);
        role.setSystem(false);
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());

        var saved = roleRepository.save(role);
        log.info("Role created: code={} id={} tenant={}", saved.getRoleCode(), saved.getId(), saved.getTenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Role getById(UUID tenantId, UUID roleId) {
        return roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Role getByCode(UUID tenantId, String roleCode) {
        return roleRepository.findByCode(tenantId, roleCode)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> listByTenant(UUID tenantId) {
        return roleRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional
    public Role update(UUID tenantId, UUID roleId, UpdateRoleCommand command) {
        var role = roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));

        if (command.roleName() != null) role.setRoleName(command.roleName());
        if (command.roleNameAr() != null) role.setRoleNameAr(command.roleNameAr());
        if (command.description() != null) role.setDescription(command.description());
        if (command.active() != null) role.setActive(command.active());
        role.setUpdatedAt(Instant.now());

        var saved = roleRepository.save(role);
        log.info("Role updated: id={} tenant={}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID roleId) {
        var role = roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));

        if (role.isSystem()) {
            throw new BusinessException(
                    ErrorCodes.Identity.ROLE_SYSTEM_IMMUTABLE,
                    "System role cannot be deleted: " + role.getRoleCode(),
                    role.getRoleCode());
        }

        rolePermissionRepository.deleteAllByRoleId(roleId);
        roleRepository.deleteById(tenantId, roleId);
        log.info("Role deleted: id={} code={} tenant={}", roleId, role.getRoleCode(), tenantId);
    }
}
