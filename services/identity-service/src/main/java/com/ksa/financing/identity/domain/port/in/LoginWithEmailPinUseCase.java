package com.ksa.financing.identity.domain.port.in;

/**
 * Email + PIN login for users onboarded via the Canada / Foreign / Guest flows.
 *
 * <p>Differs from {@link LoginWithPinUseCase} in three ways:</p>
 * <ul>
 *   <li>Lookup key is email (Keycloak username = email)</li>
 *   <li>PIN is verified against a bcrypt hash stored as Keycloak attribute {@code pin_hash}
 *       (the legacy KSA flow stored a plaintext PIN as {@code app_pin})</li>
 *   <li>Response carries the onboarding metadata that the mobile dashboard needs:
 *       {@code onboardingComplete}, {@code onboardingFlow}, {@code mobileNumber} —
 *       all sourced from Keycloak user attributes</li>
 * </ul>
 */
public interface LoginWithEmailPinUseCase {

    LoginWithEmailPinResult login(LoginWithEmailPinCommand command);

    record LoginWithEmailPinCommand(
            String email,
            String pin
    ) {}

    record LoginWithEmailPinResult(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType,
            String keycloakUserId,
            String customerId,
            String pepStatus,
            String name,
            String email,
            String mobileNumber,
            String onboardingFlow,
            boolean onboardingComplete
    ) {}
}
