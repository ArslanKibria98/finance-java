package com.ksa.financing.onboarding.shared.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.io.Serializable;
import java.util.Map;

/**
 * Generic onboarding profile activity — used by every onboarding workflow (Canada,
 * Foreign, Guest, future country flows) for Keycloak user provisioning, password-grant
 * token issuance, customer + wallet creation, and PIN persistence.
 */
@ActivityInterface
public interface OnboardingProfileActivity {

    @ActivityMethod
    KeycloakUserResult createKeycloakUser(String email, String mobileNumber, String firstName, String lastName);

    @ActivityMethod
    TokenResult issueTokenForUser(String keycloakUserId, String email, String password);

    @ActivityMethod
    CustomerProfileResult createCustomerProfile(String email, String mobileNumber,
                                                String keycloakUserId,
                                                Map<String, Object> confirmedDocumentData,
                                                String tenantId);

    @ActivityMethod
    WalletResult createWallet(String customerId, String tenantId);

    /**
     * Overload that lets the workflow pass a precomputed display name (typically
     * "{@code givenName surname}" from Facia-confirmed data). When supplied, the
     * wallet's {@code masked_name} / {@code english_first_name} / {@code english_third_name}
     * columns are populated immediately — without requiring an identity-service
     * round-trip during wallet creation.
     */
    @ActivityMethod
    WalletResult createWalletWithName(String customerId, String tenantId, String displayName);

    @ActivityMethod
    void persistPin(String keycloakUserId, String pin);

    /**
     * Final step — flips the {@code onboarding_complete} flag, records the flow type
     * and (optionally) the mobile number on the Keycloak user so downstream services
     * can read it from the JWT. Also writes a {@code COMPLETED} row to the audit table.
     */
    @ActivityMethod
    void completeOnboarding(String keycloakUserId, String flowType, String mobileNumber,
                            boolean onboardingComplete);

    record KeycloakUserResult(boolean created, String keycloakUserId, String tempPassword,
                              String failureReason) implements Serializable {}

    record TokenResult(boolean issued, String accessToken, String refreshToken,
                       long expiresIn, String tokenType,
                       String failureReason) implements Serializable {}

    record CustomerProfileResult(boolean created, String customerId, String globalUid,
                                 String failureReason) implements Serializable {}

    record WalletResult(boolean created, String walletId,
                        String failureReason) implements Serializable {}
}
