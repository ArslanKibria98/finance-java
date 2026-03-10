package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.domain.model.Role;
import com.ksa.financing.identity.domain.port.out.RoleRepository;
import com.ksa.financing.identity.infrastructure.persistence.mapper.RolePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final JpaRoleRepository jpaRoleRepository;

    @Override
    public Role save(Role role) {
        var entity = RolePersistenceMapper.toEntity(role);
        var saved = jpaRoleRepository.save(entity);
        return RolePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Role> findById(UUID tenantId, UUID roleId) {
        return jpaRoleRepository.findByTenantIdAndId(tenantId, roleId)
                .map(RolePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(UUID tenantId, String roleCode) {
        return jpaRoleRepository.findByTenantIdAndRoleCode(tenantId, roleCode)
                .map(RolePersistenceMapper::toDomain);
    }

    @Override
    public List<Role> findAllByTenant(UUID tenantId) {
        return jpaRoleRepository.findAllByTenantIdOrderByRoleCodeAsc(tenantId).stream()
                .map(RolePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(UUID tenantId, String roleCode) {
        return jpaRoleRepository.existsByTenantIdAndRoleCode(tenantId, roleCode);
    }

    @Override
    public void deleteById(UUID tenantId, UUID roleId) {
        jpaRoleRepository.deleteByTenantIdAndId(tenantId, roleId);
    }
}
