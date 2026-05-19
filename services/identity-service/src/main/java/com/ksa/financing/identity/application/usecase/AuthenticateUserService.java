package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.AuthenticateUserUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.infrastructure.blacklist.LoginGuardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    @Value("${keycloak.realm:CompanyRealm}")
    private String defaultRealm;

    private final KeycloakAdapterPort keycloakAdapter;
    private final LoginGuardService loginGuard;

    public AuthenticateUserService(KeycloakAdapterPort keycloakAdapter, LoginGuardService loginGuard) {
        this.keycloakAdapter = keycloakAdapter;
        this.loginGuard = loginGuard;
    }

    @Override
    public AuthResult authenticate(AuthCommand command) {
        // Username = NID for customers; reject pre-token if NID is blacklisted.
        loginGuard.verifyByNid(command.username());

        String realm = command.realm() != null ? command.realm() : defaultRealm;
        KeycloakAdapterPort.TokenResponse token = keycloakAdapter.authenticate(
            realm, command.username(), command.password()
        );
        return new AuthResult(token.accessToken(), token.refreshToken(), token.expiresIn(), token.name());
    }

    @Override
    public AuthResult refreshToken(String refreshToken) {
        KeycloakAdapterPort.TokenResponse token = keycloakAdapter.refreshToken(defaultRealm, refreshToken);
        return new AuthResult(token.accessToken(), token.refreshToken(), token.expiresIn(), token.name());
    }
}
