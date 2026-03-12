package com.ksa.financing.middleware.domain.port.in;

import com.ksa.financing.middleware.application.dto.BulkGrantAccessRequest;
import com.ksa.financing.middleware.application.dto.BulkGrantAccessResponse;
import com.ksa.financing.middleware.application.dto.ClientApiAccessResponse;
import com.ksa.financing.middleware.application.dto.ClientProviderAccessResponse;
import com.ksa.financing.middleware.application.dto.GrantAccessRequest;

import java.util.List;
import java.util.UUID;

public interface ManageClientAccessUseCase {

    ClientProviderAccessResponse grantProviderAccess(UUID tenantId, UUID clientId,
                                                      GrantAccessRequest request, UUID grantedBy);

    ClientApiAccessResponse grantApiAccess(UUID tenantId, UUID clientId,
                                            GrantAccessRequest request, UUID grantedBy);

    /**
     * Bulk grant access: select multiple providers, for each select specific APIs (or all),
     * and grant provider-level + API-level access in one call.
     */
    BulkGrantAccessResponse bulkGrantAccess(UUID tenantId, BulkGrantAccessRequest request, UUID grantedBy);

    void revokeProviderAccess(UUID tenantId, UUID clientId, UUID providerId);

    void revokeApiAccess(UUID tenantId, UUID clientId, UUID apiId);

    List<ClientProviderAccessResponse> listProviderAccess(UUID tenantId, UUID clientId);

    List<ClientApiAccessResponse> listApiAccess(UUID tenantId, UUID clientId);

    List<ClientApiAccessResponse> listApiAccessByProvider(UUID tenantId, UUID clientId, UUID providerId);
}
