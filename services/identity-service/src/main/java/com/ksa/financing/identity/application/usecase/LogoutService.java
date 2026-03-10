package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.LogoutUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService implements LogoutUseCase {

    private final KeycloakAdapterPort keycloakAdapter;

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    @Override
    public void logout(UUID keycloakUserId) {
        log.info("Logging out Keycloak user: {}", keycloakUserId);
        keycloakAdapter.logout(realm, keycloakUserId);
        log.info("Logout successful for user: {}", keycloakUserId);
    }
}
