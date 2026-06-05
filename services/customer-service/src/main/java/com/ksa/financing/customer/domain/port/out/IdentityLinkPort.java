package com.ksa.financing.customer.domain.port.out;

import java.util.UUID;

/**
 * Notifies identity-service to link an internal customerId to a Keycloak user
 * (sets user_identity_mapping.internal_customer_id), enabling mobile/NID/keycloakId
 * lookups that resolve to a customerId (used by wallet recipient lookup etc.).
 */
public interface IdentityLinkPort {

    void linkInternalCustomer(UUID keycloakUserId, UUID internalCustomerId);
}
