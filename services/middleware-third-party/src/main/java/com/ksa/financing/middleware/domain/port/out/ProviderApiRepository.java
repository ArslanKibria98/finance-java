package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.middleware.domain.model.ProviderApi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderApiRepository {
    ProviderApi save(ProviderApi api);
    Optional<ProviderApi> findById(UUID tenantId, UUID id);
    Optional<ProviderApi> findByCode(UUID tenantId, String code);
    List<ProviderApi> findAllByProvider(UUID tenantId, UUID providerId);
    List<ProviderApi> findAllByTenant(UUID tenantId);
    void deleteById(UUID tenantId, UUID id);
    boolean existsByCode(UUID tenantId, String code);
}
