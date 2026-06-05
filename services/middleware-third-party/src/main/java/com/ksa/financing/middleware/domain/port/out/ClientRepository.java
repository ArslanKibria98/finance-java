package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.middleware.domain.model.ApiClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {
    ApiClient save(ApiClient client);
    Optional<ApiClient> findById(UUID tenantId, UUID id);
    Optional<ApiClient> findByCode(UUID tenantId, String code);
    /**
     * Lookup a client by its code without scoping to a tenant.
     * Used by the system "/simple" execution endpoint where the caller has
     * no JWT and no tenant context — the default system client is resolved
     * by a well-known code (e.g., TEST_MOCK_CLIENT) configured per env.
     */
    Optional<ApiClient> findByCode(String code);
    List<ApiClient> findAllByTenant(UUID tenantId);
    void deleteById(UUID tenantId, UUID id);
    boolean existsByCode(UUID tenantId, String code);
    Optional<ApiClient> findBySecretKey(String secretKey);
}
