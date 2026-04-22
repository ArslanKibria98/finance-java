package com.ksa.financing.identity.adapter.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.identity.domain.port.out.CustomerLookupPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@Slf4j
public class CustomerServiceClient implements CustomerLookupPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String customerServiceUrl;

    public CustomerServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.customer-service-url}") String customerServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.customerServiceUrl = customerServiceUrl;
    }

    @Override
    public Optional<CustomerLookupResult> resolveCustomerByNationalId(String nationalId, String bearerToken) {
        String url = customerServiceUrl + "/api/v1/customers/by-nid/" + nationalId;
        log.info("Resolving customer ID from customer-service: {}", url);
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }

            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            log.info("Customer-service response status: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Customer-service returned non-success or empty body");
                return Optional.empty();
            }

            var body = objectMapper.readTree(response.getBody());

            // Handle wrapped response: { "data": { "id": "..." } } or direct { "id": "..." }
            JsonNode customerNode = body.has("data") ? body.get("data") : body;
            JsonNode idNode = customerNode.get("id");
            if (idNode != null && !idNode.isNull()) {
                String customerId = idNode.asText();
                String pepStatus = null;
                var pepStatusNode = customerNode.get("pepStatus");
                if (pepStatusNode != null && !pepStatusNode.isNull()) {
                    pepStatus = pepStatusNode.asText();
                }
                log.info("Resolved customer-service ID: {} with pepStatus={}", customerId, pepStatus);
                return Optional.of(new CustomerLookupResult(customerId, pepStatus));
            }
            log.warn("Customer-service response has no 'id' field");
            return Optional.empty();

        } catch (Exception e) {
            log.error("Customer ID resolution by NID failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }
}
