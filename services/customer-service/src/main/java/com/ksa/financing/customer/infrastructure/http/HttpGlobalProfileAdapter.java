package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.GlobalProfilePort;
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
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP adapter for communicating with the Global Profile Service.
 * Creates and retrieves global profile records for cross-service identity federation.
 */
@Component
@Slf4j
public class HttpGlobalProfileAdapter implements GlobalProfilePort {

    private final RestTemplate restTemplate;

    @Value("${services.global-profile.url:http://global-profile-service:8085}")
    private String globalProfileBaseUrl;

    public HttpGlobalProfileAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "createGlobalProfileFallback")
    public UUID createGlobalProfile(String email, String mobile, String countryCode) {
        log.info("Creating global profile for country: {}", countryCode);

        String url = globalProfileBaseUrl + "/api/v1/profiles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new java.util.HashMap<>();
        if (email != null && !email.isBlank()) {
            requestBody.put("email", email);
        }
        requestBody.put("mobile", mobile != null ? mobile : "");
        requestBody.put("primaryCountryCode", countryCode);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null) {
            // GPS wraps response in {"data": {...}, "message": "OK"}
            Map<String, Object> data = body.containsKey("data")
                    ? (Map<String, Object>) body.get("data") : body;
            if (data != null && data.containsKey("globalUid")) {
                UUID globalUid = UUID.fromString(data.get("globalUid").toString());
                log.info("Global profile created with globalUid: {}", globalUid);
                return globalUid;
            }
        }

        throw new TechnicalException(
                ErrorCodes.TECHNICAL_ERROR,
                "Invalid response from Global Profile Service: missing globalUid");
    }

    @SuppressWarnings("unused")
    private UUID createGlobalProfileFallback(String email, String mobile, String countryCode, Throwable t) {
        log.error("Global Profile Service unavailable after retries: {}", t.getMessage());
        UUID fallbackUid = UUID.randomUUID();
        log.warn("Using fallback globalUid: {} (circuit breaker open or retries exhausted)", fallbackUid);
        return fallbackUid;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "findByEmailFallback")
    public UUID findByEmail(String email) {
        log.info("Finding global profile by email hash");

        String emailHash = sha256Hash(email);
        String url = globalProfileBaseUrl + "/api/v1/profiles/by-email-hash/" + emailHash;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("globalUid")) {
            UUID globalUid = UUID.fromString(body.get("globalUid").toString());
            log.info("Global profile found with globalUid: {}", globalUid);
            return globalUid;
        }

        return null;
    }

    @SuppressWarnings("unused")
    private UUID findByEmailFallback(String email, Throwable t) {
        log.error("Global Profile Service unavailable for email lookup: {}", t.getMessage());
        return null;
    }

    /**
     * Computes the SHA-256 hash of the given input string.
     */
    private String sha256Hash(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "SHA-256 algorithm not available", e);
        }
    }
}
