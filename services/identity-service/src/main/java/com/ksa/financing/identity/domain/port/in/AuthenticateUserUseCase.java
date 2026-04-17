package com.ksa.financing.identity.domain.port.in;

public interface AuthenticateUserUseCase {
    AuthResult authenticate(AuthCommand command);
    AuthResult refreshToken(String refreshToken);

    record AuthCommand(String username, String password, String realm) {}
    record AuthResult(String accessToken, String refreshToken, long expiresIn, String name) {}
}
