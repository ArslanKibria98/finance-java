package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.PiiVaultPort;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP adapter for communicating with the PII Vault Service.
 * Handles secure storage and retrieval of Personally Identifiable Information.
 * Active when pii.vault.mock=false (default).
 */
@Component
@ConditionalOnProperty(name = "pii.vault.mock", havingValue = "false", matchIfMissing = true)
@Slf4j
public class HttpPiiVaultAdapter implements PiiVaultPort {

    private final RestTemplate restTemplate;

    @Value("${pii.vault.url:http://pii-vault-service:8086}")
    private String piiVaultBaseUrl;

    public HttpPiiVaultAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest")
    public UUID storePii(UUID globalUid, Map<String, String> piiFields) {
        log.info("Storing PII for globalUid: {}", globalUid);

        String url = piiVaultBaseUrl + "/api/v1/pii/individual";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("globalUid", globalUid.toString());
        requestBody.put("nationalId", piiFields.getOrDefault("nationalId", ""));
        requestBody.put("nationalIdType", piiFields.getOrDefault("nationalIdType", "NID"));
        requestBody.put("fullName", piiFields.getOrDefault("firstName", "") + " " + piiFields.getOrDefault("lastName", ""));
        requestBody.put("firstName", piiFields.getOrDefault("firstName", ""));
        requestBody.put("middleName", piiFields.get("middleName"));
        requestBody.put("lastName", piiFields.getOrDefault("lastName", ""));
        requestBody.put("fullNameAr", piiFields.get("fullNameAr"));
        requestBody.put("dateOfBirth", piiFields.getOrDefault("dateOfBirth", "2000-01-01"));
        requestBody.put("gender", piiFields.get("gender"));
        requestBody.put("nationalityCode", piiFields.getOrDefault("nationalityCode", "SA"));
        requestBody.put("mobile", piiFields.getOrDefault("mobile", ""));
        requestBody.put("email", piiFields.get("email"));
        requestBody.put("countryCode", piiFields.getOrDefault("countryCode", "SA"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("vaultId")) {
            UUID vaultId = UUID.fromString(body.get("vaultId").toString());
            log.info("PII stored successfully. VaultId: {}", vaultId);
            return vaultId;
        }

        log.info("PII stored successfully for globalUid: {}", globalUid);
        return globalUid;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest")
    public Map<String, String> retrievePii(UUID globalUid, String accessToken) {
        log.info("Retrieving PII for globalUid: {}", globalUid);

        String url = piiVaultBaseUrl + "/api/v1/pii/individual/" + globalUid;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null) {
            log.warn("No PII response body for globalUid: {}", globalUid);
            return Collections.emptyMap();
        }

        // PII vault wraps response in "data" key
        @SuppressWarnings("unchecked")
        Map<String, Object> data = body.containsKey("data")
                ? (Map<String, Object>) body.get("data")
                : body;

        if (data == null || data.isEmpty()) {
            log.warn("No PII data found for globalUid: {}", globalUid);
            return Collections.emptyMap();
        }

        // Convert all values to strings
        Map<String, String> result = new HashMap<>();
        for (var entry : data.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }

        log.info("PII retrieved successfully for globalUid: {} ({} fields)", globalUid, result.size());
        return result;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest")
    public void deletePii(UUID globalUid) {
        log.info("Deleting PII for globalUid: {}", globalUid);

        String url = piiVaultBaseUrl + "/api/v1/pii/individual/" + globalUid;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

        log.info("PII deleted successfully for globalUid: {}", globalUid);
    }
}
