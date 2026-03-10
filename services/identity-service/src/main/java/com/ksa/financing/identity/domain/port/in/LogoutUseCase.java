package com.ksa.financing.identity.domain.port.in;

import java.util.UUID;

public interface LogoutUseCase {
    void logout(UUID keycloakUserId);
}
