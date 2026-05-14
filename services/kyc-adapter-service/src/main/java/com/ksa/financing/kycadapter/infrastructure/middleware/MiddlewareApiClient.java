package com.ksa.financing.kycadapter.infrastructure.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Single entry point for all stub adapters that need to invoke a third-party
 * API via the middleware-third-party gateway.
 *
 * Uses the system-managed /simple endpoint so no client secret key handling
 * is required here — the middleware resolves its default client (configured
 * via MIDDLEWARE_DEFAULT_CLIENT_CODE, e.g. TEST_MOCK_CLIENT) and persists
 * each request/response into client_request_{test|dev|prod}.
 *
 *   POST {base-url}/api/v1/execute/{apiCode}/simple
 */
@Slf4j
@Component
public class MiddlewareApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public MiddlewareApiClient(@Qualifier("middlewareRestTemplate") RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                @Value("${app.middleware.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    /**
     * Execute a middleware API call and return the parsed response body.
     *
     * @param apiCode       Registered middleware API code (e.g., TAHAQUQ_VERIFY_MOBILE)
     * @param requestBody   Map serialized as the request payload
     * @param nationalId    Optional NID for audit trail (becomes X-National-Id)
     * @param mobileNumber  Optional mobile for audit trail (becomes X-Mobile-Number)
     * @param idempotencyKey Optional idempotency key (becomes X-Idempotency-Key)
     * @return The JSON node from `responseBody` if the call succeeded; throws otherwise.
     */
    public JsonNode invoke(String apiCode,
                           Map<String, Object> requestBody,
                           String nationalId,
                           String mobileNumber,
                           String idempotencyKey) {
        return invoke(apiCode, requestBody, nationalId, mobileNumber, idempotencyKey, null, null, null);
    }

    /**
     * Extended variant that propagates business context (customer / application /
     * onboarding) so the middleware can attribute cost per customer.
     *
     * @param customerId    The customer UUID (string) — sent as X-Customer-Id.
     * @param applicationId The loan-application or onboarding workflow ID — sent as X-Application-Id.
     * @param contextType   "ONBOARDING" or "APPLICATION" — sent as X-Context-Type.
     */
    public JsonNode invoke(String apiCode,
                           Map<String, Object> requestBody,
                           String nationalId,
                           String mobileNumber,
                           String idempotencyKey,
                           String customerId,
                           String applicationId,
                           String contextType) {
        String url = baseUrl + "/api/v1/execute/" + apiCode + "/simple";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (nationalId != null && !nationalId.isBlank()) {
            headers.set("X-National-Id", nationalId);
        }
        if (mobileNumber != null && !mobileNumber.isBlank()) {
            headers.set("X-Mobile-Number", mobileNumber);
        }
        if (customerId != null && !customerId.isBlank()) {
            headers.set("X-Customer-Id", customerId);
        }
        if (applicationId != null && !applicationId.isBlank()) {
            headers.set("X-Application-Id", applicationId);
        }
        if (contextType != null && !contextType.isBlank()) {
            headers.set("X-Context-Type", contextType);
        }
        headers.set("X-Idempotency-Key",
                (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : UUID.randomUUID().toString());
        headers.set("X-Caller-Service", "kyc-adapter-service");

        String body;
        try {
            body = objectMapper.writeValueAsString(requestBody != null ? requestBody : Map.of());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize middleware request body", e);
        }

        try {
            log.debug("Middleware call: apiCode={}, nid={}, mobile={}", apiCode,
                    nationalId, mobileNumber);
            var response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
            JsonNode envelope = objectMapper.readTree(response.getBody());
            JsonNode data = envelope.has("data") ? envelope.get("data") : envelope;
            JsonNode responseBody = data.get("responseBody");
            if (responseBody == null || responseBody.isNull()) {
                log.warn("Middleware returned empty responseBody for apiCode={}", apiCode);
                return objectMapper.createObjectNode();
            }
            return responseBody;
        } catch (Exception ex) {
            log.error("Middleware call failed: apiCode={}, error={}", apiCode, ex.getMessage(), ex);
            throw new MiddlewareCallException(apiCode, ex);
        }
    }

    /**
     * Convenience: return the response body parsed as a Map.
     */
    public Map<String, Object> invokeAsMap(String apiCode,
                                            Map<String, Object> requestBody,
                                            String nationalId,
                                            String mobileNumber,
                                            String idempotencyKey) {
        return invokeAsMap(apiCode, requestBody, nationalId, mobileNumber, idempotencyKey,
                null, null, null);
    }

    public Map<String, Object> invokeAsMap(String apiCode,
                                            Map<String, Object> requestBody,
                                            String nationalId,
                                            String mobileNumber,
                                            String idempotencyKey,
                                            String customerId,
                                            String applicationId,
                                            String contextType) {
        JsonNode node = invoke(apiCode, requestBody, nationalId, mobileNumber, idempotencyKey,
                customerId, applicationId, contextType);
        if (!(node instanceof ObjectNode)) {
            Map<String, Object> map = new HashMap<>();
            map.put("value", node.isValueNode() ? node.asText() : node.toString());
            return map;
        }
        try {
            return objectMapper.convertValue(node, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
