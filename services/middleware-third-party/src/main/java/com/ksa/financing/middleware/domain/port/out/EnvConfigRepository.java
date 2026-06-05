package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.middleware.domain.model.ApiEnvironmentConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvConfigRepository {
    ApiEnvironmentConfig save(ApiEnvironmentConfig config);
    Optional<ApiEnvironmentConfig> findById(UUID tenantId, UUID id);
    List<ApiEnvironmentConfig> findAllByApi(UUID tenantId, UUID apiId);
    List<ApiEnvironmentConfig> findAllByProviderId(UUID tenantId, UUID providerId);
    List<ApiEnvironmentConfig> findAllByTenant(UUID tenantId);
    void deleteById(UUID tenantId, UUID id);
}
