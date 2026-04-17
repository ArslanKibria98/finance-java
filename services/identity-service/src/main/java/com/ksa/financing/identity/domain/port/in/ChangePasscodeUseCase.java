package com.ksa.financing.identity.domain.port.in;

import java.util.UUID;

public interface ChangePasscodeUseCase {

    ChangePasscodeResult changePasscode(ChangePasscodeCommand command);

    record ChangePasscodeCommand(
        UUID keycloakUserId,
        String newPasscode,
        String confirmPasscode
    ) {}

    record ChangePasscodeResult(
        boolean success,
        String message
    ) {}
}
