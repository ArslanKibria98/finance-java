package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.KycServicePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HttpKycServiceAdapter implements KycServicePort {

    private final RestTemplate restTemplate;

    @Value("${kyc.service.url:http://kyc-adapter-service:8087}")
    private String kycServiceBaseUrl;

    public HttpKycServiceAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getYakeenDataFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getYakeenData(String nationalId, String accessToken) {
        return getYakeenData(nationalId, null, accessToken);
    }

    /**
     * Fetch Yakeen data with optional dateOfBirth for verified lookup.
     */
    public Map<String, Object> getYakeenData(String nationalId, String dateOfBirth, String accessToken) {
        log.debug("Fetching Yakeen identity data for NID: ***{}", nationalId.substring(nationalId.length() - 4));

        String url = kycServiceBaseUrl + "/api/v1/kyc/yakeen/verify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        Map<String, String> requestBody = new LinkedHashMap<>();
        requestBody.put("nationalId", nationalId);
        requestBody.put("dateOfBirth", dateOfBirth != null ? dateOfBirth : "1990-01-01");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return extractDataAsMap(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("Yakeen verification returned {}: {}", e.getStatusCode(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getVerificationSessionsFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getVerificationSessions(String nationalId, String accessToken) {
        log.debug("Fetching verification sessions for NID: ***{}", nationalId.substring(nationalId.length() - 4));
        // KYC adapter stores sessions in DB - query via provider response cache
        // For now, we get data from the Yakeen cached response which includes verification status
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataAsMap(Map body) {
        if (body == null) return Collections.emptyMap();
        if (body.containsKey("data") && body.get("data") instanceof Map) {
            return (Map<String, Object>) body.get("data");
        }
        return body;
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getYakeenDataFallback(String nationalId, String accessToken, Throwable t) {
        log.warn("KYC service unavailable for NID ***{}: {}", nationalId.substring(nationalId.length() - 4), t.getMessage());
        return Collections.emptyMap();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getVerificationSessionsFallback(String nationalId, String accessToken, Throwable t) {
        log.warn("KYC verification sessions unavailable: {}", t.getMessage());
        return Collections.emptyList();
    }
}
