package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.identity.domain.model.Permission;
import com.ksa.financing.identity.domain.port.out.PermissionRepository;
import com.ksa.financing.identity.infrastructure.persistence.entity.PermissionJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final JpaPermissionRepository jpaPermissionRepository;
    private final JpaRolePermissionRepository jpaRolePermissionRepository;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("permissionCode");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("permissionCode", "description");

    @Override
    public Permission save(Permission permission) {
        var entity = PermissionPersistenceMapper.toEntity(permission);
        var saved = jpaPermissionRepository.save(entity);
        return PermissionPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Permission> findById(UUID tenantId, UUID permissionId) {
        return jpaPermissionRepository.findByTenantIdAndId(tenantId, permissionId)
                .map(PermissionPersistenceMapper::toDomain);
    }

    @Override
    public PageResponse<Permission> findAllByTenant(UUID tenantId, PageQuery query) {
        Specification<PermissionJpaEntity> tenantSpec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        
        Specification<PermissionJpaEntity> dynamic = SpecificationBuilder.<PermissionJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<PermissionJpaEntity> page = jpaPermissionRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, PermissionPersistenceMapper::toDomain);
    }

    @Override
    public List<Permission> findByRoleId(UUID tenantId, UUID roleId) {
        return jpaPermissionRepository.findByRoleId(tenantId, roleId).stream()
                .map(PermissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String permissionCode) {
        return jpaPermissionRepository.existsByTenantIdAndPermissionCode(tenantId, permissionCode);
    }

    @Override
    public void assignToRole(UUID tenantId, UUID roleId, UUID permissionId) {
        if (!jpaRolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            var mapping = new RolePermissionJpaEntity();
            mapping.setTenantId(tenantId);
            mapping.setRoleId(roleId);
            mapping.setPermissionId(permissionId);
            jpaRolePermissionRepository.save(mapping);
        }
    }

    @Override
    public void removeFromRole(UUID tenantId, UUID roleId, UUID permissionId) {
        jpaRolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public void removeAllFromRole(UUID roleId) {
        jpaRolePermissionRepository.deleteAllByRoleId(roleId);
    }
}
