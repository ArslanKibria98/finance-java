package com.ksa.islamic.orchestration.activity.identity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for creating a Keycloak user during customer onboarding.
 *
 * After OTP verification, this activity registers the customer in Keycloak via
 * the Identity Server's onboarding-register endpoint. The customer is created with
 * the 'customer' realm role in the CompanyRealm. Returns the Keycloak user ID
 * along with initial access/refresh tokens that allow the mobile app to establish
 * an authenticated session immediately after registration.
 *
 * The globalUid links the Keycloak user to the customer profile created in later steps.
 */
@ActivityInterface
public interface KeycloakUserCreationActivity {

    @ActivityMethod
    KeycloakCreationResult createKeycloakUser(KeycloakCreationInput input);

    @ActivityMethod
    void updateKeycloakUserName(UpdateNameInput input);

    record UpdateNameInput(
        String keycloakUserId,
        String firstName
    ) {}

    record KeycloakCreationInput(
        String nationalId,
        String mobileNumber,
        String globalUid,
        String firstName
    ) {}

    record KeycloakCreationResult(
        String keycloakUserId,
        String accessToken,
        String refreshToken,
        long expiresIn,
        boolean created
    ) {}
}
