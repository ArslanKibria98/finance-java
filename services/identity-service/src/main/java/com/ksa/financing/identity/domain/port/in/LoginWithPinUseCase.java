package com.ksa.financing.identity.domain.port.in;

public interface LoginWithPinUseCase {

    LoginWithPinResult login(LoginWithPinCommand command);

    LoginWithPinResult loginWithMobile(LoginWithMobileCommand command);

    record LoginWithPinCommand(
        String nationalId,
        String pin
    ) {}

    record LoginWithMobileCommand(
        String mobileNumber,
        String pin
    ) {}

    record LoginWithPinResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String customerId,
        String nationalId,
        String mobileNumber,
        String name
    ) {}
}
