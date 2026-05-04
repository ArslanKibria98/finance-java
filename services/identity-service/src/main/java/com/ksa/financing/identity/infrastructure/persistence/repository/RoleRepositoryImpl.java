package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.identity.domain.model.Role;
import com.ksa.financing.identity.domain.port.out.RoleRepository;
import com.ksa.financing.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.ksa.financing.identity.infrastructure.persistence.mapper.RolePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final JpaRoleRepository jpaRoleRepository;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("roleCode", "active");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("roleCode", "roleNameEn", "roleNameAr");

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
    public PageResponse<Role> findAllByTenant(UUID tenantId, PageQuery query) {
        Specification<RoleJpaEntity> tenantSpec = (root, q, cb) -> cb.equal(root.get("tenantId"), tenantId);
        
        Specification<RoleJpaEntity> dynamic = SpecificationBuilder.<RoleJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<RoleJpaEntity> page = jpaRoleRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, RolePersistenceMapper::toDomain);
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
