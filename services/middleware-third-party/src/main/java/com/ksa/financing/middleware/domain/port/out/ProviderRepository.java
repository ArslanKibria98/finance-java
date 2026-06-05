package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.middleware.domain.model.ThirdPartyProvider;

import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository {
    ThirdPartyProvider save(ThirdPartyProvider provider);
    Optional<ThirdPartyProvider> findById(UUID tenantId, UUID id);
    Optional<ThirdPartyProvider> findByCode(UUID tenantId, String code);
    PageResponse<ThirdPartyProvider> findAllByTenant(UUID tenantId, PageQuery query);
    void deleteById(UUID tenantId, UUID id);
    boolean existsByCode(UUID tenantId, String code);
}
