package com.ksa.financing.identity.workflow.activity.impl;

import com.ksa.financing.identity.domain.port.in.RegisterFromOnboardingUseCase;
import com.ksa.islamic.orchestration.activity.identity.KeycloakUserCreationActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KeycloakUserCreationActivityImpl implements KeycloakUserCreationActivity {

    private final RegisterFromOnboardingUseCase registerFromOnboardingUseCase;

    @Override
    public KeycloakCreationResult createKeycloakUser(KeycloakCreationInput input) {
        log.info("Creating Keycloak user for nationalId={}", input.nationalId());
        try {
            RegisterFromOnboardingUseCase.RegisterFromOnboardingResult result =
                    registerFromOnboardingUseCase.register(
                            new RegisterFromOnboardingUseCase.RegisterFromOnboardingCommand(
                                    input.nationalId(),
                                    input.mobileNumber(),
                                    input.globalUid()
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
}
