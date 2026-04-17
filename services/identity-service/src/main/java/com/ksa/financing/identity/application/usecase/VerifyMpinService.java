package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.VerifyMpinUseCase;
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
public class VerifyMpinService implements VerifyMpinUseCase {

    private static final String PIN_ATTRIBUTE = "app_pin";

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    private final KeycloakAdapterPort keycloakAdapter;

    @Override
    public VerifyMpinResult verifyMpin(VerifyMpinCommand command) {
        log.info("Verify MPIN request for keycloakUserId={}", command.keycloakUserId());

        // Get stored PIN directly from Keycloak using the user ID from JWT
        String storedPin = keycloakAdapter.getUserAttribute(
                realm, command.keycloakUserId(), PIN_ATTRIBUTE);

        if (storedPin == null || storedPin.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_NOT_SET,
                    "MPIN has not been set for this account");
        }

        // Verify MPIN
        if (!storedPin.equals(command.mpin())) {
            log.warn("Invalid MPIN attempt for keycloakUserId={}", command.keycloakUserId());
            throw new BusinessException(
                    ErrorCodes.Identity.PIN_INVALID,
                    "Invalid MPIN provided");
        }

        log.info("MPIN verified successfully for keycloakUserId={}", command.keycloakUserId());
        return new VerifyMpinResult(true, "MPIN is valid");
    }
}
