package com.ksa.financing.ledger.infrastructure.fineract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Low-level Fineract HTTP gateway used internally by ledger-service proxy controllers.
 * <p>
 * No service outside ledger-service should call Fineract directly — they call this
 * gateway via the public {@code /api/v1/fineract-proxy/**} endpoints.
 */
@Slf4j
@Component
public class FineractGateway {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FineractProxyProperties props;

    public FineractGateway(@Qualifier("fineractRestTemplate") RestTemplate restTemplate,
                           ObjectMapper objectMapper,
                           FineractProxyProperties props) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /**
     * Performs a Fineract HTTP call and returns the raw JSON response + status code.
     * Errors propagate to caller as {@link FineractCallResult} with non-2xx status,
     * never as exceptions — proxy controller decides what to surface.
     */
    public FineractCallResult call(HttpMethod method, String relativePath, Map<String, Object> body) {
        String url = props.getBaseUrl() + relativePath;
        long start = System.currentTimeMillis();
        HttpHeaders headers = buildHeaders();

        HttpEntity<String> entity;
        try {
            String payload = body != null ? objectMapper.writeValueAsString(body) : null;
            entity = new HttpEntity<>(payload, headers);
        } catch (Exception e) {
            return FineractCallResult.error(500, "Failed to serialise request: " + e.getMessage(),
                    (int) (System.currentTimeMillis() - start));
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            int duration = (int) (System.currentTimeMillis() - start);
            JsonNode json = parseSafely(response.getBody());
            log.debug("Fineract {} {} -> {} ({}ms)", method, relativePath, response.getStatusCode(), duration);
            return FineractCallResult.success(response.getStatusCode().value(), json, duration);
        } catch (HttpStatusCodeException e) {
            int duration = (int) (System.currentTimeMillis() - start);
            log.warn("Fineract {} {} rejected: status={} body={}", method, relativePath,
                    e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            JsonNode errorJson = parseSafely(e.getResponseBodyAsString());
            return FineractCallResult.error(e.getStatusCode().value(),
                    e.getResponseBodyAsString(), errorJson, duration);
        } catch (Exception e) {
            int duration = (int) (System.currentTimeMillis() - start);
            log.error("Fineract {} {} failed: {}", method, relativePath, e.getMessage(), e);
            return FineractCallResult.error(503,
                    "Fineract unreachable: " + e.getMessage(), duration);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Fineract-Platform-TenantId", props.getTenantId());
        if (props.getUsername() != null && props.getPassword() != null) {
            String creds = props.getUsername() + ":" + props.getPassword();
            String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return headers;
    }

    private JsonNode parseSafely(String body) {
        if (body == null || body.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("raw", body);
            return node;
        }
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "...[truncated]" : s;
    }

    public record FineractCallResult(int status, JsonNode body, String errorMessage, int durationMs) {
        public static FineractCallResult success(int status, JsonNode body, int durationMs) {
            return new FineractCallResult(status, body, null, durationMs);
        }
        public static FineractCallResult error(int status, String message, int durationMs) {
            return new FineractCallResult(status, null, message, durationMs);
        }
        public static FineractCallResult error(int status, String message, JsonNode body, int durationMs) {
            return new FineractCallResult(status, body, message, durationMs);
        }
        public boolean isSuccess() { return status >= 200 && status < 300; }
    }
}
