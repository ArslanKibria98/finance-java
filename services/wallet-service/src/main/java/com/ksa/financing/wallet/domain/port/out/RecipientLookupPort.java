package com.ksa.financing.wallet.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a transfer recipient (or sender) from various lookup keys to the
 * canonical customerId, plus presentation-friendly fields used during the
 * verify-recipient step.
 */
public interface RecipientLookupPort {

    Optional<UserLookup> lookupByMobile(String mobileNumber);

    Optional<UserLookup> lookupByKeycloakUserId(UUID keycloakUserId);

    Optional<UserLookup> lookupByNationalId(String nationalId);

    Optional<UserLookup> lookupByCustomerId(UUID customerId);

    record UserLookup(
            UUID keycloakUserId,
            UUID customerId,
            UUID tenantId,
            String name,
            String firstName,
            String lastName,
            String maskedMobile,
            String status,
            boolean enabled
    ) {}
}
