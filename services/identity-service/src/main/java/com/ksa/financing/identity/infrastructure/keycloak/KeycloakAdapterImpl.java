package com.ksa.financing.identity.infrastructure.keycloak;

import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keycloak adapter implementation that communicates with Keycloak REST API.
 * <p>
 * For user creation and role assignment, this uses the Keycloak Admin REST API.
 * For authentication and token refresh, this uses the OpenID Connect token endpoint.
 * <p>
 * NOTE: This is a simplified/stub implementation. In production, consider using
 * keycloak-admin-client library for admin operations and handle error cases more robustly.
 */
@Component
@Slf4j
public class KeycloakAdapterImpl implements KeycloakAdapterPort {

    private final RestTemplate restTemplate;

    @Value("${keycloak.auth-server-url:http://keycloak:8080}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm:CompanyRealm}")
    private String defaultRealm;

    @Value("${keycloak.admin-client-id:admin-dashboard}")
    private String adminClientId;

    @Value("${keycloak.admin-client-secret:}")
    private String adminClientSecret;

    public KeycloakAdapterImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak", fallbackMethod = "createUserFallback")
    public KeycloakUser createUser(String realm, String username, String email, String password, String firstName) {
        log.info("Creating Keycloak user: {} in realm: {}", username, realm);

        String adminToken = obtainAdminToken(realm);

        String usersUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        java.util.Map<String, Object> userRepresentation = new java.util.HashMap<>();
        userRepresentation.put("username", username);
        userRepresentation.put("email", email);
        userRepresentation.put("enabled", true);
        userRepresentation.put("emailVerified", true);
        userRepresentation.put("requiredActions", List.of());
        if (firstName != null && !firstName.isBlank()) {
            userRepresentation.put("firstName", firstName);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRepresentation, headers);
        restTemplate.postForEntity(usersUrl, request, Void.class);

        // Retrieve the created user to get the Keycloak user ID
        String searchUrl = usersUrl + "?username=" + username + "&exact=true";
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<List> searchResponse = restTemplate.exchange(
                searchUrl, org.springframework.http.HttpMethod.GET, getRequest, List.class
        );

        if (searchResponse.getBody() != null && !searchResponse.getBody().isEmpty()) {
            Map<String, Object> createdUser = (Map<String, Object>) searchResponse.getBody().get(0);
            UUID keycloakUserId = UUID.fromString((String) createdUser.get("id"));

            // Keycloak 26+ ignores credentials in user creation payload.
            // Set password separately via the reset-password endpoint.
            String resetPwUrl = usersUrl + "/" + keycloakUserId + "/reset-password";
            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            );
            HttpEntity<Map<String, Object>> pwRequest = new HttpEntity<>(credential, headers);
            restTemplate.put(resetPwUrl, pwRequest);

            log.info("Keycloak user created successfully with ID: {}", keycloakUserId);
            return new KeycloakUser(keycloakUserId, username);
        }

        UUID fallbackId = UUID.randomUUID();
        log.warn("Could not retrieve created Keycloak user, using generated ID: {}", fallbackId);
        return new KeycloakUser(fallbackId, username);
    }

    @SuppressWarnings("unused")
    private KeycloakUser createUserFallback(String realm, String username, String email, String password, String firstName, Throwable t) {
        log.error("Keycloak unavailable for user creation after retries: {}", t.getMessage());
        UUID mockId = UUID.randomUUID();
        log.warn("Returning mock Keycloak user with ID: {} (circuit breaker fallback)", mockId);
        return new KeycloakUser(mockId, username);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak", fallbackMethod = "assignRoleFallback")
    public void assignRole(String realm, UUID keycloakUserId, String roleName) {
        log.info("Assigning role '{}' to user {} in realm {}", roleName, keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Try to get existing role from Keycloak
        String roleUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        ResponseEntity<Map> roleResponse;
        try {
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            roleResponse = restTemplate.exchange(
                    roleUrl, org.springframework.http.HttpMethod.GET, getRequest, Map.class
            );
        } catch (Exception e) {
            // Role doesn't exist in Keycloak — create it
            log.info("Role '{}' not found in Keycloak, creating it in realm '{}'", roleName, realm);
            String createRoleUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/roles";
            Map<String, Object> newRole = Map.of("name", roleName);
            HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(newRole, headers);
            restTemplate.postForEntity(createRoleUrl, createRequest, Void.class);
            log.info("Role '{}' created in Keycloak realm '{}'", roleName, realm);

            // Now fetch the created role
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            roleResponse = restTemplate.exchange(
                    roleUrl, org.springframework.http.HttpMethod.GET, getRequest, Map.class
            );
        }

        if (roleResponse.getBody() == null) {
            log.warn("Role '{}' not found in realm '{}' even after creation attempt", roleName, realm);
            return;
        }

        // Assign role to user
        String assignUrl = keycloakBaseUrl + "/admin/realms/" + realm
                + "/users/" + keycloakUserId + "/role-mappings/realm";
        HttpEntity<List<Map>> assignRequest = new HttpEntity<>(List.of(roleResponse.getBody()), headers);
        restTemplate.postForEntity(assignUrl, assignRequest, Void.class);

        log.info("Role '{}' assigned successfully to user {}", roleName, keycloakUserId);
    }

    @SuppressWarnings("unused")
    private void assignRoleFallback(String realm, UUID keycloakUserId, String roleName, Throwable t) {
        log.error("Keycloak unavailable for role assignment: {}", t.getMessage());
        log.warn("Role assignment skipped (circuit breaker fallback) - role: {}, user: {}", roleName, keycloakUserId);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenResponse authenticate(String realm, String username, String password) {
        log.info("Authenticating user: {} in realm: {}", username, realm);

        String tokenUrl = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", "openid");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");
                Number expiresIn = (Number) body.get("expires_in");
                String name = extractNameFromJwt(accessToken);
                log.info("User authenticated successfully: {}", username);
                return new TokenResponse(accessToken, refreshToken, expiresIn != null ? expiresIn.longValue() : 300L, name);
            }

            throw new TechnicalException(
                    ErrorCodes.Identity.SESSION_INVALID,
                    "Empty response from Keycloak token endpoint");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Authentication failed for user: {} - Invalid credentials", username);
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Invalid username or password");
        } catch (HttpClientErrorException e) {
            log.error("Keycloak authentication error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Authentication failed: " + e.getStatusCode());
        }
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenResponse refreshToken(String realm, String refreshToken) {
        log.info("Refreshing token in realm: {}", realm);

        String tokenUrl = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);
        formData.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null) {
            String newAccessToken = (String) body.get("access_token");
            String newRefreshToken = (String) body.get("refresh_token");
            Number expiresIn = (Number) body.get("expires_in");
            String name = extractNameFromJwt(newAccessToken);
            log.info("Token refreshed successfully");
            return new TokenResponse(newAccessToken, newRefreshToken, expiresIn != null ? expiresIn.longValue() : 300L, name);
        }

        throw new TechnicalException(
                ErrorCodes.Identity.SESSION_INVALID,
                "Empty response from Keycloak token endpoint");
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak", fallbackMethod = "logoutFallback")
    public void logout(String realm, UUID keycloakUserId) {
        log.info("Logging out user {} from realm {}", keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);
        String logoutUrl = keycloakBaseUrl + "/admin/realms/" + realm
                + "/users/" + keycloakUserId + "/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        restTemplate.postForEntity(logoutUrl, request, Void.class);

        log.info("User {} logged out successfully", keycloakUserId);
    }

    @SuppressWarnings("unused")
    private void logoutFallback(String realm, UUID keycloakUserId, Throwable t) {
        log.error("Keycloak unavailable for logout: {}", t.getMessage());
        log.warn("Logout skipped (circuit breaker fallback) - user: {}", keycloakUserId);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak", fallbackMethod = "setUserAttributeFallback")
    @SuppressWarnings("unchecked")
    public void setUserAttribute(String realm, UUID keycloakUserId, String attributeName, String attributeValue) {
        log.info("Setting attribute '{}' on user {} in realm {}", attributeName, keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);

        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                userUrl, org.springframework.http.HttpMethod.GET, getRequest, Map.class
        );

        Map<String, Object> userRep = userResponse.getBody();
        if (userRep == null) {
            log.warn("User {} not found in realm {}", keycloakUserId, realm);
            return;
        }

        Map<String, List<String>> attributes = (Map<String, List<String>>) userRep.get("attributes");
        if (attributes == null) {
            attributes = new java.util.HashMap<>();
        }
        attributes.put(attributeName, List.of(attributeValue));
        userRep.put("attributes", attributes);

        HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(userRep, headers);
        restTemplate.put(userUrl, updateRequest);

        log.info("Attribute '{}' set successfully on user {}", attributeName, keycloakUserId);
    }

    @SuppressWarnings("unused")
    private void setUserAttributeFallback(String realm, UUID keycloakUserId, String attributeName, String attributeValue, Throwable t) {
        log.error("Keycloak unavailable for attribute set: {}", t.getMessage());
        log.warn("Attribute set skipped (circuit breaker fallback) - attribute: {}, user: {}", attributeName, keycloakUserId);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public void setUserAttributes(String realm, UUID keycloakUserId, Map<String, String> newAttributes) {
        log.info("Setting {} attributes on user {} in realm {}", newAttributes.keySet(), keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // GET current user representation
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                userUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> userRep = new java.util.HashMap<>(userResponse.getBody());

        // Merge new attributes with existing ones (single GET-then-PUT avoids lost-update race condition)
        Map<String, List<String>> existing = (Map<String, List<String>>) userRep.get("attributes");
        Map<String, List<String>> merged = existing != null ? new java.util.HashMap<>(existing) : new java.util.HashMap<>();
        newAttributes.forEach((k, v) -> merged.put(k, List.of(v)));
        userRep.put("attributes", merged);

        restTemplate.put(userUrl, new HttpEntity<>(userRep, headers));
        log.info("Attributes {} set successfully on user {}", newAttributes.keySet(), keycloakUserId);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public String getUserAttribute(String realm, UUID keycloakUserId, String attributeName) {
        log.info("Getting attribute '{}' for user {} in realm {}", attributeName, keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                userUrl, org.springframework.http.HttpMethod.GET, getRequest, Map.class
        );

        Map<String, Object> userRep = userResponse.getBody();
        if (userRep == null) {
            log.warn("User {} not found in realm {}", keycloakUserId, realm);
            return null;
        }

        Map<String, List<String>> attributes = (Map<String, List<String>>) userRep.get("attributes");
        if (attributes == null || !attributes.containsKey(attributeName)) {
            log.warn("Attribute '{}' not found for user {}", attributeName, keycloakUserId);
            return null;
        }

        List<String> values = attributes.get(attributeName);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void resetPassword(String realm, UUID keycloakUserId, String newPassword) {
        log.info("Resetting password for user {} in realm {}", keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);
        String resetPwUrl = keycloakBaseUrl + "/admin/realms/" + realm
                + "/users/" + keycloakUserId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(credential, headers);
        restTemplate.put(resetPwUrl, request);

        log.info("Password reset successfully for user {}", keycloakUserId);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public void updateUserFirstName(String realm, UUID keycloakUserId, String firstName) {
        log.info("Updating firstName for user {} in realm {}", keycloakUserId, realm);

        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                userUrl, org.springframework.http.HttpMethod.GET, getRequest, Map.class);

        Map<String, Object> userRep = new java.util.HashMap<>(userResponse.getBody());
        userRep.put("firstName", firstName != null ? firstName : "");
        userRep.remove("lastName");

        HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(userRep, headers);
        restTemplate.put(userUrl, updateRequest);

        log.info("firstName updated successfully for user {}", keycloakUserId);
    }

    /**
     * Obtains an admin access token using client credentials grant.
     */
    private String obtainAdminToken(String realm) {
        String tokenUrl = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("access_token")) {
            return (String) body.get("access_token");
        }

        throw new TechnicalException(
                ErrorCodes.TECHNICAL_ERROR,
                "Failed to obtain admin token from Keycloak");
    }

    /**
     * Decodes the JWT payload (base64url) and extracts the "name" claim.
     * Returns null if the token is malformed or the claim is absent.
     */
    @SuppressWarnings("unchecked")
    private String extractNameFromJwt(String accessToken) {
        try {
            if (accessToken == null) return null;
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) return null;
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);
            Object name = claims.get("name");
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            log.warn("Could not extract name from JWT: {}", e.getMessage());
            return null;
        }
    }
}
