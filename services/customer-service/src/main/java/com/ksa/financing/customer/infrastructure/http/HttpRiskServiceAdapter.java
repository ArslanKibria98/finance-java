package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.RiskServicePort;
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

@Component
@Slf4j
public class HttpRiskServiceAdapter implements RiskServicePort {

    private final RestTemplate restTemplate;

    @Value("${risk.service.url:http://risk-service:8090}")
    private String riskServiceBaseUrl;

    public HttpRiskServiceAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getAssessmentsFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAssessmentsByEntity(String entityReference, String accessToken) {
        log.debug("Fetching risk assessments for entity: {}", entityReference);
        String url = riskServiceBaseUrl + "/api/v1/risk/assessments/entity/" + entityReference;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsList(response.getBody());
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getEntityStatusFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEntityStatus(String entityReference, String accessToken) {
        log.debug("Fetching entity status for: {}", entityReference);
        String url = riskServiceBaseUrl + "/api/v1/risk/entity-status/" + entityReference + "/current";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsMap(response.getBody());
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getEntityStatusHistoryFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEntityStatusHistory(String entityReference, String accessToken) {
        log.debug("Fetching entity status history for: {}", entityReference);
        String url = riskServiceBaseUrl + "/api/v1/risk/entity-status/" + entityReference + "/history";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsList(response.getBody());
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getScoreBreakdownFallback")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getScoreBreakdown(String sessionId, String accessToken) {
        log.debug("Fetching score breakdown for session: {}", sessionId);
        String url = riskServiceBaseUrl + "/api/v1/risk/assessments/" + sessionId + "/breakdown";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsMap(response.getBody());
    }

    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getScoreBreakdownListFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getScoreBreakdownList(String sessionId, String accessToken) {
        log.debug("Fetching score breakdown list for session: {}", sessionId);
        String url = riskServiceBaseUrl + "/api/v1/risk/assessments/" + sessionId + "/breakdown";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsList(response.getBody());
    }

    @Override
    @Retry(name = "internal-rest")
    @CircuitBreaker(name = "internal-rest", fallbackMethod = "getAssessmentAnswersFallback")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAssessmentAnswers(String sessionId, String accessToken) {
        log.debug("Fetching assessment answers for session: {}", sessionId);
        String url = riskServiceBaseUrl + "/api/v1/risk/assessments/" + sessionId + "/answers";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, authEntity(accessToken), Map.class);
        return extractDataAsList(response.getBody());
    }

    // ---- Helpers ----

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

    // ---- Fallbacks ----

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getAssessmentsFallback(String entityReference, String accessToken, Throwable t) {
        log.warn("Risk service unavailable for entity {}: {}", entityReference, t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getEntityStatusFallback(String entityReference, String accessToken, Throwable t) {
        log.warn("Risk service entity status unavailable for {}: {}", entityReference, t.getMessage());
        return Collections.emptyMap();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getEntityStatusHistoryFallback(String entityReference, String accessToken, Throwable t) {
        log.warn("Risk service status history unavailable for {}: {}", entityReference, t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private Map<String, Object> getScoreBreakdownFallback(String sessionId, String accessToken, Throwable t) {
        log.warn("Risk service score breakdown unavailable for session {}: {}", sessionId, t.getMessage());
        return Collections.emptyMap();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getScoreBreakdownListFallback(String sessionId, String accessToken, Throwable t) {
        log.warn("Risk service score breakdown list unavailable for session {}: {}", sessionId, t.getMessage());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private List<Map<String, Object>> getAssessmentAnswersFallback(String sessionId, String accessToken, Throwable t) {
        log.warn("Risk service assessment answers unavailable for session {}: {}", sessionId, t.getMessage());
        return Collections.emptyList();
    }
}
