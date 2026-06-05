package com.ksa.financing.identity.domain.port.out;

import com.ksa.financing.identity.domain.model.Module;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository {
    List<Module> findAllByTenant(UUID tenantId);
    List<Module> findCatalogByTenant(UUID tenantId);
    Optional<Module> findById(UUID tenantId, UUID id);
    Optional<Module> findByCode(UUID tenantId, String moduleCode);
    Module save(Module module);
}
