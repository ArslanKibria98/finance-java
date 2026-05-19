package com.ksa.financing.identity.domain.port.in;

import java.util.UUID;

public interface LinkUserCustomerUseCase {

    LinkResult link(UUID keycloakUserId, UUID internalCustomerId);

    record LinkResult(boolean linked, UUID keycloakUserId, UUID internalCustomerId, String reason) {}
}
