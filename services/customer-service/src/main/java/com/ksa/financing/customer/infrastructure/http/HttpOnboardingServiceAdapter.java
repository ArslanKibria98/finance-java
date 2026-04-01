package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.OnboardingServicePort;
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
import java.util.Map;

@Component
@Slf4j
public class HttpOnboardingServiceAdapter implements OnboardingServicePort {

    private final RestTemplate restTemplate;

    @Value("${onboarding.service.url:http://onboarding-workflow-service:8089}")
    private String onboardingServiceBaseUrl;

    public HttpOnboardingServiceAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getOnboardingStatusFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOnboardingStatus(String nationalId, String accessToken) {
        log.debug("Fetching onboarding status for NID: ***{}", nationalId.substring(nationalId.length() - 4));

        String url = onboardingServiceBaseUrl + "/api/v1/onboarding/status?nationalId=" + nationalId;

        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            if (body == null) return Collections.emptyMap();
            if (body.containsKey("data") && body.get("data") instanceof Map) {
                return (Map<String, Object>) body.get("data");
            }
            return body;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.debug("Onboarding status not found for NID: ***{}", nationalId.substring(nationalId.length() - 4));
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getOnboardingStatusFallback(String nationalId, String accessToken, Throwable t) {
        log.warn("Onboarding service unavailable: {}", t.getMessage());
        return Collections.emptyMap();
    }
}
