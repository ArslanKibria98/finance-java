package com.ksa.financing.identity.infrastructure.keycloak;

import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
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
import org.springframework.web.util.UriUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keycloak adapter implementation that communicates with Keycloak REST API.
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

    @Value("${keycloak.user-lookup-retry-max-attempts:5}")
    private int userLookupRetryMaxAttempts;

    @Value("${keycloak.user-lookup-retry-delay-ms:250}")
    private long userLookupRetryDelayMs;

    public KeycloakAdapterImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
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
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        UUID keycloakUserId;
        try {
            ResponseEntity<Void> createResponse = restTemplate.postForEntity(usersUrl, request, Void.class);
            keycloakUserId = extractUserIdFromLocation(createResponse.getHeaders().getLocation())
                    .orElseGet(() -> resolveCreatedUserId(usersUrl, getRequest, username, email));
            log.info("Keycloak user created successfully with ID: {}", keycloakUserId);
        } catch (HttpClientErrorException.Conflict conflict) {
            log.warn("Keycloak user already exists for username/email, reusing existing record: {}",
                    conflict.getResponseBodyAsString());
            keycloakUserId = resolveCreatedUserId(usersUrl, getRequest, username, email);
        }

        // Set password separately
        String resetPwUrl = usersUrl + "/" + keycloakUserId + "/reset-password";
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );
        HttpEntity<Map<String, Object>> pwRequest = new HttpEntity<>(credential, headers);
        restTemplate.put(resetPwUrl, pwRequest);

        return new KeycloakUser(keycloakUserId, username);
    }

    private java.util.Optional<UUID> extractUserIdFromLocation(URI location) {
        if (location == null) return java.util.Optional.empty();
        String path = location.getPath();
        if (path == null || path.isBlank()) return java.util.Optional.empty();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash + 1 >= path.length()) return java.util.Optional.empty();
        String id = path.substring(lastSlash + 1);
        try { return java.util.Optional.of(UUID.fromString(id)); }
        catch (IllegalArgumentException ex) { return java.util.Optional.empty(); }
    }

    private UUID resolveCreatedUserId(String usersUrl, HttpEntity<Void> getRequest, String username, String email) {
        for (int attempt = 1; attempt <= userLookupRetryMaxAttempts; attempt++) {
            var byUsername = searchUser(usersUrl, getRequest, "username", username);
            if (byUsername != null && !byUsername.isEmpty()) {
                Map<String, Object> user = (Map<String, Object>) byUsername.get(0);
                return UUID.fromString((String) user.get("id"));
            }
            var byEmail = searchUser(usersUrl, getRequest, "email", email);
            if (byEmail != null && !byEmail.isEmpty()) {
                Map<String, Object> user = (Map<String, Object>) byEmail.get(0);
                return UUID.fromString((String) user.get("id"));
            }
            if (attempt < userLookupRetryMaxAttempts) {
                try { Thread.sleep(userLookupRetryDelayMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new TechnicalException(ErrorCodes.TECHNICAL_ERROR, "Interrupted"); }
            }
        }
        throw new TechnicalException(ErrorCodes.TECHNICAL_ERROR, "Keycloak user not found after creation");
    }

    @SuppressWarnings("rawtypes")
    private List searchUser(String usersUrl, HttpEntity<Void> getRequest, String paramName, String paramValue) {
        String url = usersUrl + "?" + paramName + "=" + UriUtils.encodeQueryParam(paramValue, StandardCharsets.UTF_8) + "&exact=true";
        ResponseEntity<List> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, getRequest, List.class);
        return response.getBody();
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void assignRole(String realm, UUID keycloakUserId, String roleName) {
        log.info("Assigning role '{}' to user {} in realm {}", roleName, keycloakUserId, realm);
        String adminToken = obtainAdminToken(realm);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        String roleUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        ResponseEntity<Map> roleResponse;
        try {
            roleResponse = restTemplate.exchange(roleUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        } catch (Exception e) {
            log.info("Creating role '{}' in Keycloak", roleName);
            restTemplate.postForEntity(keycloakBaseUrl + "/admin/realms/" + realm + "/roles", new HttpEntity<>(Map.of("name", roleName), headers), Void.class);
            roleResponse = restTemplate.exchange(roleUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        }

        if (roleResponse.getBody() == null) throw new TechnicalException(ErrorCodes.TECHNICAL_ERROR, "Role not found");

        String assignUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/role-mappings/realm";
        restTemplate.postForEntity(assignUrl, new HttpEntity<>(List.of(roleResponse.getBody()), headers), Void.class);
        log.info("Role '{}' assigned successfully", roleName);
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

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(formData, headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                String accessToken = (String) body.get("access_token");
                String refreshToken = (String) body.get("refresh_token");
                Number expiresIn = (Number) body.get("expires_in");
                return new TokenResponse(accessToken, refreshToken, expiresIn != null ? expiresIn.longValue() : 300L, extractNameFromJwt(accessToken));
            }
            throw new TechnicalException(ErrorCodes.Identity.SESSION_INVALID, "Empty response from Keycloak");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Invalid username or password");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "Authentication failed: " + e.getMessage());
        }
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public TokenResponse refreshToken(String realm, String refreshToken) {
        String tokenUrl = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);
        formData.add("refresh_token", refreshToken);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(formData, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body != null) {
            String newAccessToken = (String) body.get("access_token");
            return new TokenResponse(newAccessToken, (String) body.get("refresh_token"), 
                    ((Number) body.get("expires_in")).longValue(), extractNameFromJwt(newAccessToken));
        }
        throw new TechnicalException(ErrorCodes.Identity.SESSION_INVALID, "Failed to refresh token");
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void logout(String realm, UUID keycloakUserId) {
        String adminToken = obtainAdminToken(realm);
        String logoutUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/logout";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        restTemplate.postForEntity(logoutUrl, new HttpEntity<>(headers), Void.class);
        log.info("User {} logged out", keycloakUserId);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public void setUserAttribute(String realm, UUID keycloakUserId, String attributeName, String attributeValue) {
        log.info("Setting attribute '{}' for user {}", attributeName, keycloakUserId);
        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        ResponseEntity<Map> userResponse = restTemplate.exchange(userUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> userRep = userResponse.getBody();
        if (userRep == null) throw new NotFoundException("User", keycloakUserId.toString());

        Map<String, List<String>> attributes = (Map<String, List<String>>) userRep.get("attributes");
        if (attributes == null) attributes = new java.util.HashMap<>();
        attributes.put(attributeName, List.of(attributeValue));
        userRep.put("attributes", attributes);

        restTemplate.put(userUrl, new HttpEntity<>(userRep, headers));
        log.info("Attribute '{}' set successfully", attributeName);
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public void setUserAttributes(String realm, UUID keycloakUserId, Map<String, String> newAttributes) {
        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        ResponseEntity<Map> userResponse = restTemplate.exchange(userUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> userRep = new java.util.HashMap<>(userResponse.getBody());
        Map<String, List<String>> attributes = (Map<String, List<String>>) userRep.get("attributes");
        if (attributes == null) attributes = new java.util.HashMap<>();
        Map<String, List<String>> merged = new java.util.HashMap<>(attributes);
        newAttributes.forEach((k, v) -> merged.put(k, List.of(v)));
        userRep.put("attributes", merged);

        restTemplate.put(userUrl, new HttpEntity<>(userRep, headers));
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public String getUserAttribute(String realm, UUID keycloakUserId, String attributeName) {
        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        ResponseEntity<Map> userResponse = restTemplate.exchange(userUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> userRep = userResponse.getBody();
        if (userRep == null) return null;
        Map<String, List<String>> attributes = (Map<String, List<String>>) userRep.get("attributes");
        if (attributes == null || !attributes.containsKey(attributeName)) return null;
        List<String> values = attributes.get(attributeName);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void resetPassword(String realm, UUID keycloakUserId, String newPassword) {
        String adminToken = obtainAdminToken(realm);
        String resetPwUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        Map<String, Object> credential = Map.of("type", "password", "value", newPassword, "temporary", false);
        restTemplate.put(resetPwUrl, new HttpEntity<>(credential, headers));
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public void updateUserFirstName(String realm, UUID keycloakUserId, String firstName) {
        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        ResponseEntity<Map> userResponse = restTemplate.exchange(userUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> userRep = new java.util.HashMap<>(userResponse.getBody());
        userRep.put("firstName", firstName != null ? firstName : "");
        userRep.remove("lastName");
        restTemplate.put(userUrl, new HttpEntity<>(userRep, headers));
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public KeycloakUserDetails getUserDetails(String realm, UUID keycloakUserId) {
        String adminToken = obtainAdminToken(realm);
        String userUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        ResponseEntity<Map> response = restTemplate.exchange(userUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) return null;
        return new KeycloakUserDetails(keycloakUserId, (String) body.get("username"), (String) body.get("firstName"), (String) body.get("lastName"), (String) body.get("email"), Boolean.TRUE.equals(body.get("enabled")));
    }

    @Override
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    @SuppressWarnings("unchecked")
    public java.util.Optional<KeycloakUser> findUserByEmail(String realm, String email) {
        if (email == null || email.isBlank()) return java.util.Optional.empty();
        String adminToken = obtainAdminToken(realm);
        String usersUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        var rows = searchUser(usersUrl, getRequest, "email", email);
        if (rows == null || rows.isEmpty()) return java.util.Optional.empty();
        Map<String, Object> user = (Map<String, Object>) rows.get(0);
        return java.util.Optional.of(new KeycloakUser(
                UUID.fromString((String) user.get("id")),
                (String) user.get("username")
        ));
    }

    private String obtainAdminToken(String realm) {
        String tokenUrl = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(formData, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("access_token")) return (String) body.get("access_token");
        throw new TechnicalException(ErrorCodes.TECHNICAL_ERROR, "Failed to obtain admin token");
    }

    @SuppressWarnings("unchecked")
    private String extractNameFromJwt(String accessToken) {
        try {
            if (accessToken == null) return null;
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) return null;
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper().readValue(new String(decoded, StandardCharsets.UTF_8), Map.class);
            Object name = claims.get("name");
            return name != null ? name.toString() : null;
        } catch (Exception e) { return null; }
    }
}
