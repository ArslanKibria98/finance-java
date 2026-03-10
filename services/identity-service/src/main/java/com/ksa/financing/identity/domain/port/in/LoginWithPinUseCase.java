package com.ksa.financing.identity.domain.port.in;

public interface LoginWithPinUseCase {

    LoginWithPinResult login(LoginWithPinCommand command);

    record LoginWithPinCommand(
        String nationalId,
        String pin
    ) {}

    record LoginWithPinResult(
        String accessToken,
        String refreshToken,
        long expiresIn
    ) {}
}
