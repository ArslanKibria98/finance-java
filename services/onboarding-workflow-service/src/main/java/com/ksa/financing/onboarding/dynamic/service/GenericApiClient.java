package com.ksa.financing.onboarding.dynamic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GenericApiClient {
    private static final Logger log = LoggerFactory.getLogger(GenericApiClient.class);
    private final RestTemplate restTemplate;

    public GenericApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Executes a generic API call based on the provided configuration.
     */
    public Map<String, Object> execute(String url, String method, Map<String, String> customHeaders, Map<String, Object> queryParams, Map<String, Object> body) {
        log.info("Executing generic API call to {} using method {}", url, method);
        
        // Build URL with Query Parameters
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        String finalUrl = builder.toUriString();

        // Build Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (customHeaders != null) {
            customHeaders.forEach(headers::set);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(finalUrl, httpMethod, entity, Map.class);
            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.error("Failed to execute generic API call: {}", e.getMessage());
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }
}
