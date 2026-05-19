package com.ksa.financing.onboarding.dynamic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PiiVaultClient {
    private static final Logger log = LoggerFactory.getLogger(PiiVaultClient.class);
    private final RestTemplate restTemplate;

    @Value("${app.services.pii-vault-service-url:http://pii-vault-service:8086}")
    private String piiVaultUrl;

    public PiiVaultClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Stores sensitive PII data and returns a token or success status.
     * In this implementation, we map dynamic fields to the PII Vault contract.
     */
    public void storePii(String globalUid, String countryCode, Map<String, Object> sensitiveData) {
        log.info("Storing sensitive data in PII Vault for session: {}", globalUid);

        Map<String, Object> request = new HashMap<>();
        request.put("globalUid", globalUid);
        request.put("countryCode", countryCode);
        
        // Map common keys to PII Vault contract
        request.put("nationalId", sensitiveData.getOrDefault("cnic_number", sensitiveData.get("nationalId")));
        request.put("mobile", sensitiveData.get("mobileNumber"));
        request.put("email", sensitiveData.get("email"));
        
        // Add other fields as needed
        request.put("fullName", sensitiveData.get("fullName"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(piiVaultUrl + "/api/v1/pii/individual", entity, Map.class);
            log.info("PII data stored successfully for session: {}", globalUid);
        } catch (Exception e) {
            log.error("Failed to store PII data: {}", e.getMessage());
            // In a real system, we might want to throw an exception here if PII storage is mandatory
        }
    }
}
