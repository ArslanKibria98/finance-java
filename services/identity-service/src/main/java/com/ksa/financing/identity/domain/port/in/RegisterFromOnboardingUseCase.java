package com.ksa.financing.identity.domain.port.in;

public interface RegisterFromOnboardingUseCase {

    RegisterFromOnboardingResult register(RegisterFromOnboardingCommand command);

    record RegisterFromOnboardingCommand(
        String nationalId,
        String mobileNumber,
        String globalUid,
        String firstName
    ) {}

    record RegisterFromOnboardingResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String keycloakUserId
    ) {}
}
