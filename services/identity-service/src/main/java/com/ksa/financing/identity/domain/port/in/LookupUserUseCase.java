package com.ksa.financing.identity.domain.port.in;

import java.util.Optional;
import java.util.UUID;

public interface LookupUserUseCase {

    Optional<UserLookupResult> lookupByMobile(String mobileNumber);

    Optional<UserLookupResult> lookupByKeycloakUserId(UUID keycloakUserId);

    Optional<UserLookupResult> lookupByNationalId(String nationalId);

    record UserLookupResult(
            UUID keycloakUserId,
            UUID customerId,
            UUID tenantId,
            String name,
            String firstName,
            String lastName,
            String mobileNumber,
            String nationalId,
            String status,
            boolean enabled
    ) {}
}
