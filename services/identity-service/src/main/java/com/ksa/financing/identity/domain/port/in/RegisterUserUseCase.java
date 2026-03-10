package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.identity.domain.model.UserIdentity;
import java.util.UUID;

public interface RegisterUserUseCase {
    UserIdentity register(RegisterUserCommand command);

    record RegisterUserCommand(
        UUID tenantId,
        String keycloakRealm,
        String username,
        String email,
        String mobileNumber,
        String password
    ) {}
}
