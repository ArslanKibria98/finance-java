package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.Permission;
import com.ksa.financing.identity.domain.port.in.ManagePermissionUseCase;
import com.ksa.financing.identity.domain.port.out.ModuleRepository;
import com.ksa.financing.identity.domain.port.out.PermissionRepository;
import com.ksa.financing.identity.domain.port.out.RoleRepository;
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
public class ManagePermissionService implements ManagePermissionUseCase {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;

    @Override
    @Transactional
    public Permission create(CreatePermissionCommand command) {
        if (permissionRepository.existsByCode(command.tenantId(), command.permissionCode())) {
            throw new BusinessException(
                    ErrorCodes.Identity.PERMISSION_DUPLICATE,
                    "Permission with code already exists: " + command.permissionCode(),
                    command.permissionCode());
        }

        var perm = new Permission();
        perm.setTenantId(command.tenantId());
        perm.setPermissionCode(command.permissionCode());
        perm.setPermissionName(command.permissionName());
        perm.setDescription(command.description());
        perm.setResourceType(command.resourceType());
        // Auto-resolve moduleId from resourceType
        if (command.resourceType() != null) {
            moduleRepository.findByCode(command.tenantId(), command.resourceType())
                    .ifPresent(module -> perm.setModuleId(module.getId()));
        }
        perm.setAction(command.action());
        perm.setActive(true);
        perm.setCreatedAt(Instant.now());
        perm.setUpdatedAt(Instant.now());

        var saved = permissionRepository.save(perm);
        log.info("Permission created: code={} id={} tenant={}", saved.getPermissionCode(), saved.getId(), saved.getTenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Permission getById(UUID tenantId, UUID permissionId) {
        return permissionRepository.findById(tenantId, permissionId)
                .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> listByTenant(UUID tenantId) {
        return permissionRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> listByRole(UUID tenantId, UUID roleId) {
        roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));
        return permissionRepository.findByRoleId(tenantId, roleId);
    }

    @Override
    @Transactional
    public Permission update(UUID tenantId, UUID permissionId, UpdatePermissionCommand command) {
        var perm = permissionRepository.findById(tenantId, permissionId)
                .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));

        if (command.permissionName() != null) perm.setPermissionName(command.permissionName());
        if (command.description() != null) perm.setDescription(command.description());
        if (command.active() != null) perm.setActive(command.active());
        perm.setUpdatedAt(Instant.now());

        var saved = permissionRepository.save(perm);
        log.info("Permission updated: id={} tenant={}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public void assignToRole(UUID tenantId, UUID roleId, UUID permissionId) {
        roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));
        permissionRepository.findById(tenantId, permissionId)
                .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));

        permissionRepository.assignToRole(tenantId, roleId, permissionId);
        log.info("Permission {} assigned to role {} in tenant {}", permissionId, roleId, tenantId);
    }

    @Override
    @Transactional
    public void removeFromRole(UUID tenantId, UUID roleId, UUID permissionId) {
        permissionRepository.removeFromRole(tenantId, roleId, permissionId);
        log.info("Permission {} removed from role {} in tenant {}", permissionId, roleId, tenantId);
    }

    @Override
    @Transactional
    public void syncRolePermissions(UUID tenantId, UUID roleId, List<UUID> permissionIds) {
        roleRepository.findById(tenantId, roleId)
                .orElseThrow(() -> NotFoundException.forEntity("Role", roleId.toString()));

        // Remove all existing permissions for this role
        permissionRepository.removeAllFromRole(roleId);

        // Assign new permissions
        for (var permissionId : permissionIds) {
            permissionRepository.findById(tenantId, permissionId)
                    .orElseThrow(() -> NotFoundException.forEntity("Permission", permissionId.toString()));
            permissionRepository.assignToRole(tenantId, roleId, permissionId);
        }

        log.info("Synced {} permissions to role {} in tenant {}", permissionIds.size(), roleId, tenantId);
    }
}
