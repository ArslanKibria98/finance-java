package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.tenant.TenantConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantConfigRepository {

    TenantConfig save(TenantConfig config);

    Optional<TenantConfig> findById(UUID tenantId, UUID id);

    Optional<TenantConfig> findByTenantId(UUID tenantId);

    List<TenantConfig> findAll();

    boolean existsByTenantId(UUID tenantId);
}
