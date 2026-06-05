package com.ksa.financing.middleware.domain.port.out;

import com.ksa.financing.middleware.domain.model.ClientApiAccess;
import com.ksa.financing.middleware.domain.model.ClientProviderAccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientAccessRepository {

    // Provider access
    ClientProviderAccess saveProviderAccess(ClientProviderAccess access);
    Optional<ClientProviderAccess> findProviderAccess(UUID tenantId, UUID clientId, UUID providerId);
    List<ClientProviderAccess> findProviderAccessByClient(UUID tenantId, UUID clientId);
    void deleteProviderAccess(UUID tenantId, UUID clientId, UUID providerId);

    // API access
    ClientApiAccess saveApiAccess(ClientApiAccess access);
    Optional<ClientApiAccess> findApiAccess(UUID tenantId, UUID clientId, UUID apiId);
    List<ClientApiAccess> findApiAccessByClient(UUID tenantId, UUID clientId);
    List<ClientApiAccess> findApiAccessByClientAndProvider(UUID tenantId, UUID clientId, UUID providerId);
    void deleteApiAccess(UUID tenantId, UUID clientId, UUID apiId);
}
