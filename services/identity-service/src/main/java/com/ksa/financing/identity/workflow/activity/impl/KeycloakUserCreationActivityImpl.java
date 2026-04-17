package com.ksa.financing.identity.workflow.activity.impl;

import com.ksa.financing.identity.domain.port.in.RegisterFromOnboardingUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.islamic.orchestration.activity.identity.KeycloakUserCreationActivity;
import io.temporal.activity.Activity;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class KeycloakUserCreationActivityImpl implements KeycloakUserCreationActivity {

    private final RegisterFromOnboardingUseCase registerFromOnboardingUseCase;
    private final KeycloakAdapterPort keycloakAdapter;

    public KeycloakUserCreationActivityImpl(RegisterFromOnboardingUseCase registerFromOnboardingUseCase,
                                            KeycloakAdapterPort keycloakAdapter) {
        this.registerFromOnboardingUseCase = registerFromOnboardingUseCase;
        this.keycloakAdapter = keycloakAdapter;
    }

    @Override
    public KeycloakCreationResult createKeycloakUser(KeycloakCreationInput input) {
        log.info("Creating Keycloak user for nationalId={}", input.nationalId());
        try {
            RegisterFromOnboardingUseCase.RegisterFromOnboardingResult result =
                    registerFromOnboardingUseCase.register(
                            new RegisterFromOnboardingUseCase.RegisterFromOnboardingCommand(
                                    input.nationalId(),
                                    input.mobileNumber(),
                                    input.globalUid(),
                                    input.firstName()
                            )
                    );

            return new KeycloakCreationResult(
                    result.keycloakUserId(),
                    result.accessToken(),
                    result.refreshToken(),
                    result.expiresIn(),
                    true
            );
        } catch (Exception e) {
            log.error("Keycloak user creation failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }

    @Override
    public void updateKeycloakUserName(UpdateNameInput input) {
        log.info("Updating Keycloak user name for keycloakUserId={}", input.keycloakUserId());
        try {
            if (input.keycloakUserId() == null || input.firstName() == null || input.firstName().isBlank()) {
                log.warn("Skipping name update — keycloakUserId or firstName is blank");
                return;
            }
            keycloakAdapter.updateUserFirstName("CompanyRealm",
                    UUID.fromString(input.keycloakUserId()), input.firstName());
            log.info("Keycloak user name updated successfully for keycloakUserId={}", input.keycloakUserId());
        } catch (Exception e) {
            log.error("Failed to update Keycloak user name for keycloakUserId={}: {}",
                    input.keycloakUserId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
