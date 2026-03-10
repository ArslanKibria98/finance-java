package com.ksa.financing.identity.infrastructure.persistence.repository;

import com.ksa.financing.identity.domain.model.Module;
import com.ksa.financing.identity.domain.port.out.ModuleRepository;
import com.ksa.financing.identity.infrastructure.persistence.mapper.ModulePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ModuleRepositoryImpl implements ModuleRepository {

    private final JpaModuleRepository jpaModuleRepository;

    @Override
    public List<Module> findAllByTenant(UUID tenantId) {
        return jpaModuleRepository.findByTenantIdOrderByDisplayOrder(tenantId)
                .stream()
                .map(ModulePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Module> findById(UUID tenantId, UUID id) {
        return jpaModuleRepository.findByTenantIdAndId(tenantId, id)
                .map(ModulePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Module> findByCode(UUID tenantId, String moduleCode) {
        return jpaModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .map(ModulePersistenceMapper::toDomain);
    }

    @Override
    public Module save(Module module) {
        var entity = ModulePersistenceMapper.toEntity(module);
        var saved = jpaModuleRepository.save(entity);
        return ModulePersistenceMapper.toDomain(saved);
    }
}
