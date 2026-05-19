package com.ksa.financing.onboarding.dynamic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CustomerServiceClient {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceClient.class);
    private final RestTemplate restTemplate;

    @Value("${app.services.customer-service-url:http://customer-service:8084}")
    private String customerServiceUrl;

    public CustomerServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Creates a permanent customer record (CIF) from onboarding data.
     */
    public String createCif(Map<String, Object> customerData) {
        log.info("Creating permanent CIF in Customer-Service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    customerServiceUrl + "/api/v1/customers", entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String cifId = (String) response.getBody().get("customerId");
                log.info("CIF created successfully: {}", cifId);
                return cifId;
            }
            throw new RuntimeException("Failed to create CIF: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Error calling Customer-Service: {}", e.getMessage());
            throw new RuntimeException("Customer-Service error: " + e.getMessage());
        }
    }
}
