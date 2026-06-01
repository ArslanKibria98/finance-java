package com.ksa.financing.onboarding.shared.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Generic Keycloak admin client used by every onboarding flow (KSA, Canada, Foreign,
 * Guest, …) that needs to provision a Keycloak user, assign a realm role, and exchange
 * credentials for a JWT via the password grant.
 */
@Component
public class OnboardingKeycloakClient {

    private static final Logger log = LoggerFactory.getLogger(OnboardingKeycloakClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;

    public OnboardingKeycloakClient(RestTemplate restTemplate,
                                    @Value("${keycloak.base-url}") String baseUrl,
                                    @Value("${keycloak.realm}") String realm,
                                    @Value("${keycloak.admin-client-id}") String adminClientId,
                                    @Value("${keycloak.admin-client-secret}") String adminClientSecret) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    public String realm() {
        return realm;
    }

    public String obtainAdminToken() {
        String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(form, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("Failed to obtain Keycloak admin token");
        }
        return (String) body.get("access_token");
    }

    /** Creates user (or reuses existing on 409) and sets the password. Returns the Keycloak userId. */
    public String createUserWithPassword(String email, String password, String firstName, String lastName) {
        String adminToken = obtainAdminToken();
        String usersUrl = baseUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> userRep = new java.util.HashMap<>();
        userRep.put("username", email);
        userRep.put("email", email);
        userRep.put("enabled", true);
        userRep.put("emailVerified", true);
        userRep.put("requiredActions", List.of());
        if (firstName != null && !firstName.isBlank()) userRep.put("firstName", firstName);
        if (lastName != null && !lastName.isBlank()) userRep.put("lastName", lastName);

        String keycloakUserId;
        try {
            ResponseEntity<Void> createResp = restTemplate.postForEntity(usersUrl, new HttpEntity<>(userRep, headers), Void.class);
            keycloakUserId = extractUserIdFromLocation(createResp.getHeaders().getLocation());
            if (keycloakUserId == null) {
                keycloakUserId = lookupUserIdByEmail(usersUrl, headers, email);
            }
            log.info("Keycloak user created: id={} email={}", keycloakUserId, email);
        } catch (HttpClientErrorException.Conflict conflict) {
            log.info("Keycloak user already exists for email={}, reusing", email);
            keycloakUserId = lookupUserIdByEmail(usersUrl, headers, email);
        }

        // Reset password
        String resetUrl = usersUrl + "/" + keycloakUserId + "/reset-password";
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );
        restTemplate.put(resetUrl, new HttpEntity<>(credential, headers));

        return keycloakUserId;
    }

    /** Assigns a realm role to the user. Creates the role first if it does not exist. */
    public void assignRealmRole(String keycloakUserId, String roleName) {
        String adminToken = obtainAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        String roleUrl = baseUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        ResponseEntity<Map> roleResp;
        try {
            roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        } catch (HttpClientErrorException.NotFound nf) {
            log.info("Creating Keycloak realm role: {}", roleName);
            restTemplate.postForEntity(
                    baseUrl + "/admin/realms/" + realm + "/roles",
                    new HttpEntity<>(Map.of("name", roleName), headers),
                    Void.class);
            roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        }

        if (roleResp.getBody() == null) {
            throw new IllegalStateException("Keycloak role lookup failed: " + roleName);
        }

        String assignUrl = baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/role-mappings/realm";
        restTemplate.postForEntity(assignUrl, new HttpEntity<>(List.of(roleResp.getBody()), headers), Void.class);
        log.info("Assigned realm role={} to user={}", roleName, keycloakUserId);
    }

    /**
     * Updates / merges custom user attributes (single-valued) on an existing Keycloak user.
     * Keycloak stores attributes as {@code Map<String, List<String>>}, so we wrap each value
     * in a singleton list.
     *
     * <p>Used to mark {@code onboarding_complete}, {@code onboarding_flow}, {@code mobile_number},
     * {@code pin_hash}, etc. — values then flow into the JWT via Keycloak protocol mappers.</p>
     */
    @SuppressWarnings("unchecked")
    public void setUserAttributes(String keycloakUserId, Map<String, String> attributes) {
        if (keycloakUserId == null || attributes == null || attributes.isEmpty()) return;
        String adminToken = obtainAdminToken();
        String userUrl = baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Fetch current rep to preserve existing attributes
        ResponseEntity<Map> getResp = restTemplate.exchange(userUrl, HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        Map<String, Object> userRep = getResp.getBody();
        if (userRep == null) {
            throw new IllegalStateException("Keycloak user not found: " + keycloakUserId);
        }

        Map<String, List<String>> attrs = (Map<String, List<String>>) userRep.getOrDefault(
                "attributes", new java.util.HashMap<String, List<String>>());
        if (attrs == null) attrs = new java.util.HashMap<>();
        for (Map.Entry<String, String> e : attributes.entrySet()) {
            attrs.put(e.getKey(), List.of(e.getValue()));
        }
        userRep.put("attributes", attrs);

        restTemplate.put(userUrl, new HttpEntity<>(userRep, headers));
        log.info("Keycloak user attributes updated: id={} keys={}", keycloakUserId, attributes.keySet());
    }

    /** Password grant — returns a real Keycloak-issued JWT. */
    public TokenPayload passwordGrant(String username, String password) {
        String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid");

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(form, headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("Empty token response from Keycloak");
        }
        Number expiresIn = (Number) body.getOrDefault("expires_in", 300);
        String tokenType = (String) body.getOrDefault("token_type", "Bearer");
        return new TokenPayload(
                (String) body.get("access_token"),
                (String) body.get("refresh_token"),
                expiresIn.longValue(),
                tokenType
        );
    }

    private String extractUserIdFromLocation(URI location) {
        if (location == null) return null;
        String path = location.getPath();
        if (path == null || path.isBlank()) return null;
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx + 1 >= path.length()) return null;
        return path.substring(idx + 1);
    }

    @SuppressWarnings("unchecked")
    private String lookupUserIdByEmail(String usersUrl, HttpHeaders headers, String email) {
        String url = usersUrl + "?email=" + UriUtils.encodeQueryParam(email, StandardCharsets.UTF_8) + "&exact=true";
        ResponseEntity<List> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), List.class);
        List<Map<String, Object>> body = resp.getBody();
        if (body == null || body.isEmpty()) {
            throw new IllegalStateException("Keycloak user not found after creation: " + email);
        }
        return (String) body.get(0).get("id");
    }

    public record TokenPayload(String accessToken, String refreshToken, long expiresIn, String tokenType) {}
}
