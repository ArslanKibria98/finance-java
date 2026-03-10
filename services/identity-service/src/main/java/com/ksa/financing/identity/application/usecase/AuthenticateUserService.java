package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.AuthenticateUserUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final KeycloakAdapterPort keycloakAdapter;

    public AuthenticateUserService(KeycloakAdapterPort keycloakAdapter) {
        this.keycloakAdapter = keycloakAdapter;
    }

    @Override
    public AuthResult authenticate(AuthCommand command) {
        String realm = command.realm() != null ? command.realm() : "CompanyRealm";
        KeycloakAdapterPort.TokenResponse token = keycloakAdapter.authenticate(
            realm, command.username(), command.password()
        );
        return new AuthResult(token.accessToken(), token.refreshToken(), token.expiresIn());
    }

    @Override
    public AuthResult refreshToken(String refreshToken) {
        KeycloakAdapterPort.TokenResponse token = keycloakAdapter.refreshToken("CompanyRealm", refreshToken);
        return new AuthResult(token.accessToken(), token.refreshToken(), token.expiresIn());
    }
}
