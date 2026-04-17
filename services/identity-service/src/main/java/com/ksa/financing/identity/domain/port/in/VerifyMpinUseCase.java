package com.ksa.financing.identity.domain.port.in;

public interface VerifyMpinUseCase {

    VerifyMpinResult verifyMpin(VerifyMpinCommand command);

    record VerifyMpinCommand(
        java.util.UUID keycloakUserId,
        String mpin
    ) {}

    record VerifyMpinResult(
        boolean valid,
        String message
    ) {}
}
