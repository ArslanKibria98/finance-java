package com.ksa.financing.identity.domain.port.out;

import java.util.Optional;

public interface CustomerLookupPort {

    /**
     * Resolve the customer-service customer ID by National ID.
     */
    Optional<CustomerLookupResult> resolveCustomerByNationalId(String nationalId, String bearerToken);

    /**
     * Fetch customer-service profile by customer ID. Used by email-PIN login (Canada/Foreign/Guest)
     * where the user has no National ID; the customerId comes from {@code user_identity_mapping}.
     */
    Optional<CustomerLookupResult> resolveCustomerById(String customerId, String bearerToken);

    record CustomerLookupResult(
            String customerId,
            String pepStatus,
            String name
    ) {}
}
