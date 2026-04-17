package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.ChangePasscodeUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePasscodeService implements ChangePasscodeUseCase {

    private static final String PIN_ATTRIBUTE = "app_pin";

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    private final KeycloakAdapterPort keycloakAdapter;

    @Override
    public ChangePasscodeResult changePasscode(ChangePasscodeCommand command) {
        log.info("Change passcode request for keycloakUserId={}", command.keycloakUserId());

        // Validate new passcode matches confirm
        if (!command.newPasscode().equals(command.confirmPasscode())) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "New passcode and confirm passcode do not match");
        }

        // Set new PIN
        keycloakAdapter.setUserAttribute(realm, command.keycloakUserId(), PIN_ATTRIBUTE, command.newPasscode());

        log.info("Passcode changed successfully for keycloakUserId={}", command.keycloakUserId());
        return new ChangePasscodeResult(true, "Passcode changed successfully");
    }
}
