package com.ksa.islamic.orchestration.activity.identity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for setting a customer's app PIN during onboarding.
 *
 * After the wallet is created and the welcome notification is sent, the customer
 * is prompted to set a 6-digit PIN. This activity stores the PIN as a user
 * attribute on the Keycloak user entity (plain text for now; encryption TBD).
 *
 * PIN validation rules are enforced on the controller side before the signal is sent.
 */
@ActivityInterface
public interface SetPinActivity {

    @ActivityMethod
    SetPinResult setPin(SetPinInput input);

    record SetPinInput(
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
