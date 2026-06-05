package com.ksa.financing.identity.domain.port.out;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface KeycloakAdapterPort {
    KeycloakUser createUser(String realm, String username, String email, String password, String firstName);
    void assignRole(String realm, UUID keycloakUserId, String roleName);
    TokenResponse authenticate(String realm, String username, String password);
    TokenResponse refreshToken(String realm, String refreshToken);
    void logout(String realm, UUID keycloakUserId);
    void setUserAttribute(String realm, UUID keycloakUserId, String attributeName, String attributeValue);
    /** Sets multiple attributes in a single GET-then-PUT to avoid race conditions / lost updates. */
    void setUserAttributes(String realm, UUID keycloakUserId, Map<String, String> attributes);
    String getUserAttribute(String realm, UUID keycloakUserId, String attributeName);
    void resetPassword(String realm, UUID keycloakUserId, String newPassword);
    void updateUserFirstName(String realm, UUID keycloakUserId, String firstName);
    /** Fetch top-level Keycloak user fields (firstName, lastName, email, enabled). */
    KeycloakUserDetails getUserDetails(String realm, UUID keycloakUserId);
    /** Lookup a Keycloak user by their email (exact match). */
    Optional<KeycloakUser> findUserByEmail(String realm, String email);

    record KeycloakUser(UUID keycloakUserId, String username) {}
    record TokenResponse(String accessToken, String refreshToken, long expiresIn, String name) {}
    record KeycloakUserDetails(UUID keycloakUserId, String username, String firstName,
                               String lastName, String email, boolean enabled) {}
}
