package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.LendingServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class HttpLendingServiceAdapter implements LendingServicePort {

    private final RestTemplate restTemplate;

    @Value("${lending.service.url:http://lending-service:8097}")
    private String lendingServiceBaseUrl;

    public HttpLendingServiceAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getLoanApplicationsFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLoanApplicationsByCustomerId(UUID customerId, String accessToken) {
        log.debug("Fetching loan applications for customer: {}", customerId);
        String url = lendingServiceBaseUrl + "/api/v1/loan-applications/customer/" + customerId;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsList(response.getBody());
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getLoansFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLoansByCustomerId(UUID customerId, String accessToken) {
        log.debug("Fetching loans for customer: {}", customerId);
        String url = lendingServiceBaseUrl + "/api/v1/loans/customer/" + customerId;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsList(response.getBody());
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getOverviewFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomerLoanOverview(UUID customerId, String accessToken) {
        log.debug("Fetching loan overview for customer: {}", customerId);
        String url = lendingServiceBaseUrl + "/api/v1/loans/customer/" + customerId + "/overview";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsMap(response.getBody());
    }

    private HttpEntity<Void> authEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }
        return new HttpEntity<>(headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataAsMap(Map body) {
        if (body == null) return Collections.emptyMap();
        if (body.containsKey("data") && body.get("data") instanceof Map) {
            return (Map<String, Object>) body.get("data");
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataAsList(Map body) {
        if (body == null) return Collections.emptyList();
        if (body.containsKey("data") && body.get("data") instanceof List) {
            return (List<Map<String, Object>>) body.get("data");
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getLoanApplicationsFallback(UUID customerId, String accessToken, Throwable t) {
        log.warn("Lending service unavailable for customer {}: {}", customerId, t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getLoansFallback(UUID customerId, String accessToken, Throwable t) {
        log.warn("Lending service loans unavailable for customer {}: {}", customerId, t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getOverviewFallback(UUID customerId, String accessToken, Throwable t) {
        log.warn("Lending service overview unavailable for customer {}: {}", customerId, t.getMessage());
        return Collections.emptyMap();
    }
}
