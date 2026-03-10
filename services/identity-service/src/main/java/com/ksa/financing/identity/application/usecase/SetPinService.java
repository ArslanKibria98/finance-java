package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.SetPinUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetPinService implements SetPinUseCase {

    private static final String REALM = "CompanyRealm";
    private static final String PIN_ATTRIBUTE = "app_pin";

    private final KeycloakAdapterPort keycloakAdapter;

    @Override
    public SetPinResult setPin(SetPinCommand command) {
        log.info("Setting app PIN for nationalId={}", command.nationalId());

        try {
            UUID keycloakUserId = UUID.fromString(command.keycloakUserId());

            // Store PIN as a user attribute in Keycloak (plain text for now)
            keycloakAdapter.setUserAttribute(REALM, keycloakUserId, PIN_ATTRIBUTE, command.pin());

            log.info("App PIN set successfully for nationalId={}", command.nationalId());
            return new SetPinResult(true, "PIN set successfully");

        } catch (Exception e) {
            log.error("Failed to set app PIN for nationalId={}: {}", command.nationalId(), e.getMessage(), e);
            return new SetPinResult(false, "Failed to set PIN: " + e.getMessage());
        }
    }
}
