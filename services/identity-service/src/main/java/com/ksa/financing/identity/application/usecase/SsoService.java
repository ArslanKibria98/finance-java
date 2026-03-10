package com.ksa.financing.identity.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.identity.application.dto.SsoLoginUrlResponse;
import com.ksa.financing.identity.application.dto.SsoTokenExchangeRequest;
import com.ksa.financing.identity.application.dto.SsoTokenResponse;
import com.ksa.financing.identity.domain.port.in.SsoUseCase;
import com.ksa.financing.identity.domain.port.out.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SSO Service — Pura Authorization Code Flow with PKCE handle karta hai.
 *
 * PKCE kya hai?
 *   Proof Key for Code Exchange — public clients ke liye security layer.
 *   code_verifier (random secret) → SHA256 hash → code_challenge
 *   Challenge Keycloak ko bhejte hain, verifier baad mein token exchange mein.
 *   Agar koi code intercept kare to bhi token nahi le sakta (verifier ke bina).
 *
 * Redis kyun?
 *   code_verifier server pe rehna chahiye (frontend pe expose nahi hona chahiye).
 *   State UUID se linked hai — 10 min TTL, one-time use.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoService implements SsoUseCase {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmployeeRepository employeeRepository;

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.internal-url}")
    private String keycloakInternalUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.sso.client-id}")
    private String clientId;

    @Value("${keycloak.sso.redirect-uri}")
    private String redirectUri;

    // Redis key prefix — state UUID se PKCE verifier dhundne ke liye
    private static final String PKCE_KEY_PREFIX = "sso:pkce:";

    // 10 minute — user ko itne waqt mein login karna hoga
    private static final Duration PKCE_TTL = Duration.ofMinutes(10);

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1: Login URL generate karo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public SsoLoginUrlResponse generateLoginUrl() {
        // 1a. Random state generate karo (CSRF protection + Redis key)
        String state = UUID.randomUUID().toString();

        // 1b. PKCE pair banao
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // 1c. Verifier Redis mein state ke sath store karo (10 min TTL)
        redisTemplate.opsForValue().set(PKCE_KEY_PREFIX + state, codeVerifier, PKCE_TTL);

        // 1d. Keycloak authorization URL banao
        String authUrl = UriComponentsBuilder
                .fromHttpUrl(keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();

        log.info("SSO login URL generated for state: {}", state);
        return new SsoLoginUrlResponse(authUrl, state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2: Code ko token se exchange karo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public SsoTokenResponse exchangeToken(SsoTokenExchangeRequest request) {
        // 2a. Redis se verifier nikalo (state key use karke)
        String redisKey = PKCE_KEY_PREFIX + request.state();
        String codeVerifier = redisTemplate.opsForValue().get(redisKey);

        if (codeVerifier == null) {
            log.warn("PKCE verifier not found for state: {} — expired or invalid", request.state());
            throw new IllegalStateException(
                    "Login session expired or invalid. Please login again."
            );
        }

        // 2b. One-time use — Redis se delete karo
        redisTemplate.delete(redisKey);

        // 2c. Keycloak token endpoint call karo (internal URL — ensures iss matches service config)
        String tokenUrl = keycloakInternalUrl + "/realms/" + realm
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", request.code());
        body.add("code_verifier", codeVerifier);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUrl,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> tokenData = response.getBody();
            if (tokenData == null) {
                throw new IllegalStateException("Empty response from Keycloak token endpoint");
            }

            String accessToken = (String) tokenData.get("access_token");

            // 2d. Roles JWT se nikalo (frontend routing ke liye)
            List<String> roles = extractRolesFromJwt(accessToken);

            // 2e. Employee ka roleId nikalo (keycloakUserId = JWT sub claim)
            UUID roleId = null;
            UUID keycloakUserId = extractSubFromJwt(accessToken);
            if (keycloakUserId != null) {
                roleId = employeeRepository.findByKeycloakUserId(keycloakUserId)
                        .map(emp -> emp.getRoleId())
                        .orElse(null);
            }

            log.info("SSO token exchange successful. Roles: {}, roleId: {}", roles, roleId);

            return new SsoTokenResponse(
                    accessToken,
                    (String) tokenData.get("refresh_token"),
                    (Integer) tokenData.getOrDefault("expires_in", 3600),
                    "Bearer",
                    roles,
                    roleId
            );

        } catch (HttpClientErrorException e) {
            log.error("Keycloak token exchange failed: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Invalid authorization code. Please login again.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PKCE Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 32 random bytes → Base64URL encoded string (no padding)
     * Example: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
     */
    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 hash of verifier → Base64URL encoded (S256 method)
     * Keycloak verify karta hai: SHA256(verifier) == code_challenge
     */
    private String generateCodeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * JWT se sub (Keycloak user ID) extract karo.
     */
    @SuppressWarnings("unchecked")
    private UUID extractSubFromJwt(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) return null;

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            String sub = (String) claims.get("sub");
            return sub != null ? UUID.fromString(sub) : null;
        } catch (Exception e) {
            log.warn("Could not extract sub from JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT ka middle part (payload) decode karke realm_access.roles nikalo.
     * JWT = header.payload.signature — sirf payload decode karna hai.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromJwt(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) return Collections.emptyList();

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess == null) return Collections.emptyList();

            return (List<String>) realmAccess.getOrDefault("roles", Collections.emptyList());
        } catch (Exception e) {
            log.warn("Could not extract roles from JWT: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
