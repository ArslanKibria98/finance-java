package com.ksa.financing.identity.domain.port.in;

public interface SetPinUseCase {

    SetPinResult setPin(SetPinCommand command);

    record SetPinCommand(
        String keycloakUserId,
        String nationalId,
        String pin,
        String tenantId
    ) {}

    record SetPinResult(
        boolean pinSet,
        String message
    ) {}
}
