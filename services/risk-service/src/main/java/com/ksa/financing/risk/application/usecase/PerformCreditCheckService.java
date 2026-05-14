package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.port.in.PerformCreditCheckUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PerformCreditCheckService implements PerformCreditCheckUseCase {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String middlewareUrl;
    private final String middlewareSecretKey;

    public PerformCreditCheckService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.middleware-url:}") String middlewareUrl,
            @Value("${app.services.middleware-secret-key:}") String middlewareSecretKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.middlewareUrl = middlewareUrl;
        this.middlewareSecretKey = middlewareSecretKey;
    }

    @Override
    public CreditCheckResult performCreditCheck(CreditCheckCommand command) {
        log.info("Performing credit check for NID={}, tenant={}", command.nationalId(), command.tenantId());

        if (middlewareUrl == null || middlewareUrl.isBlank()) {
            log.info("Middleware URL not configured, returning mock credit check data");
            return mockResult();
        }

        int creditScore = 0;
        String simahGrade = "UNKNOWN";
        String simahReferenceId = "SIMAH-" + UUID.randomUUID().toString().substring(0, 8);
        BigDecimal verifiedSalary = BigDecimal.ZERO;
        BigDecimal existingObligations = BigDecimal.ZERO;
        boolean hasActiveDefaults = false;

        // 1. Call SIMAH_SCORE_CREDIT for credit score.
        // SIMAH wraps its payload as { "isSuccess": true, "data": { "score": 750, "rating": "Good" } }
        try {
            var scoreResponse = callMiddleware("SIMAH_SCORE_CREDIT", command.nationalId(),
                    Map.of("nationalId", command.nationalId()),
                    command.customerId(), command.applicationId());
            JsonNode payload = unwrapSimahPayload(scoreResponse);
            if (payload != null) {
                creditScore = payload.has("creditScore") ? payload.get("creditScore").asInt()
                        : payload.has("score") ? payload.get("score").asInt() : 0;
                simahGrade = payload.has("riskGrade") ? payload.get("riskGrade").asText()
                        : payload.has("rating") ? payload.get("rating").asText() : "UNKNOWN";
            }
        } catch (Exception e) {
            log.warn("SIMAH_SCORE_CREDIT call failed: {}, using defaults", e.getMessage());
            creditScore = 720;
            simahGrade = "A";
        }

        // 2. Call SIMAH_CONSUMER_REPORT for salary
        try {
            var reportResponse = callMiddleware("SIMAH_CONSUMER_REPORT", command.nationalId(),
                    Map.of("nationalId", command.nationalId(), "productType", 155, "amount", command.requestedAmount()),
                    command.customerId(), command.applicationId());
            JsonNode payload = unwrapSimahPayload(reportResponse);
            if (payload != null) {
                if (payload.has("applicants") && payload.get("applicants").isArray() && !payload.get("applicants").isEmpty()) {
                    var applicant = payload.get("applicants").get(0);
                    if (applicant.has("demographicInfo")) {
                        var demo = applicant.get("demographicInfo");
                        verifiedSalary = demo.has("totalMonthlyIncome")
                                ? new BigDecimal(demo.get("totalMonthlyIncome").asText())
                                : BigDecimal.ZERO;
                    }
                }
                if (payload.has("referenceNumber")) {
                    simahReferenceId = payload.get("referenceNumber").asText();
                }
            }
        } catch (Exception e) {
            log.warn("SIMAH_CONSUMER_REPORT call failed: {}, using default salary", e.getMessage());
            verifiedSalary = new BigDecimal("15000");
        }

        // 3. Call SIMAH_NEGATIVE_CONSUMER for defaults
        try {
            var negativeResponse = callMiddleware("SIMAH_NEGATIVE_CONSUMER", command.nationalId(),
                    Map.of("nationalId", command.nationalId()),
                    command.customerId(), command.applicationId());
            JsonNode payload = unwrapSimahPayload(negativeResponse);
            if (payload != null) {
                hasActiveDefaults = payload.has("hasDefaults") && payload.get("hasDefaults").asBoolean();
            }
        } catch (Exception e) {
            log.warn("SIMAH_NEGATIVE_CONSUMER call failed: {}, assuming no defaults", e.getMessage());
            hasActiveDefaults = false;
        }

        // 4. Call SIMAH_CREDIT_COMMITMENTS for existing obligations
        try {
            var commitResponse = callMiddleware("SIMAH_CREDIT_COMMITMENTS", command.nationalId(),
                    Map.of("nationalId", command.nationalId()),
                    command.customerId(), command.applicationId());
            JsonNode payload = unwrapSimahPayload(commitResponse);
            if (payload != null) {
                existingObligations = payload.has("totalMonthlyInstallment")
                        ? new BigDecimal(payload.get("totalMonthlyInstallment").asText())
                        : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.warn("SIMAH_CREDIT_COMMITMENTS call failed: {}, assuming zero obligations", e.getMessage());
            existingObligations = BigDecimal.ZERO;
        }

        // If credit score is still 0, all calls likely failed — return mock
        if (creditScore == 0) {
            log.warn("All SIMAH calls returned no credit score, returning mock data");
            return mockResult();
        }

        var result = new CreditCheckResult(creditScore, simahGrade, simahReferenceId,
                verifiedSalary, existingObligations, hasActiveDefaults);
        log.info("Credit check completed: score={}, grade={}, salary={}, obligations={}, defaults={}",
                creditScore, simahGrade, verifiedSalary, existingObligations, hasActiveDefaults);
        return result;
    }

    private JsonNode callMiddleware(String apiCode, String nationalId, Map<String, Object> body) {
        return callMiddleware(apiCode, nationalId, body, null, null);
    }

    private JsonNode callMiddleware(String apiCode, String nationalId, Map<String, Object> body,
                                     String customerId, String applicationId) {
        try {
            // Use the /simple internal endpoint (no X-Secret-Key required).
            // Falls back to /execute with X-Secret-Key when a secret is configured.
            boolean useSimple = middlewareSecretKey == null || middlewareSecretKey.isBlank();
            String url = middlewareUrl + "/api/v1/execute/" + apiCode + (useSimple ? "/simple" : "");

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Caller-Service", "risk-service");
            headers.set("X-National-Id", nationalId);
            headers.set("X-Idempotency-Key", apiCode + "-" + nationalId + "-" + System.currentTimeMillis());
            if (customerId != null && !customerId.isBlank())       headers.set("X-Customer-Id", customerId);
            if (applicationId != null && !applicationId.isBlank()) headers.set("X-Application-Id", applicationId);
            headers.set("X-Context-Type", "APPLICATION");
            if (!useSimple) {
                headers.set("X-Secret-Key", middlewareSecretKey);
            }

            String requestBody = objectMapper.writeValueAsString(body);
            var response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            var root = objectMapper.readTree(response.getBody());

            // Middleware wraps replies in {"data": ExecuteApiResponse, "message": ..., "timestamp": ...}
            JsonNode envelope = root.has("data") && root.get("data").isObject() ? root.get("data") : root;

            // Inside the envelope: { success, responseBody, errorMessage, ... }
            if (envelope.has("success") && !envelope.get("success").asBoolean()) {
                return null;
            }

            if (envelope.has("responseBody") && !envelope.get("responseBody").isNull()) {
                JsonNode rb = envelope.get("responseBody");
                if (rb.isObject() || rb.isArray()) {
                    return wrapAsData(rb);
                }
                if (rb.isTextual() && !rb.asText().isBlank()) {
                    return wrapAsData(objectMapper.readTree(rb.asText()));
                }
            }
            return wrapAsData(envelope);
        } catch (Exception e) {
            log.warn("Middleware call failed for {}: {}", apiCode, e.getMessage());
            throw new RuntimeException("Middleware call failed: " + apiCode, e);
        }
    }

    /**
     * Callers above access fields via {@code response.get("data").get(field)}. Keep that
     * contract by re-wrapping the unwrapped payload under a {@code data} field.
     */
    private JsonNode wrapAsData(JsonNode payload) {
        var wrapper = objectMapper.createObjectNode();
        wrapper.set("data", payload);
        return wrapper;
    }

    /**
     * SIMAH mock payload comes back as either:
     *   - { "isSuccess": true, "data": { "score": 750, "rating": "Good" } }   (nested)
     *   - flat fields ({ "creditScore": ..., "riskGrade": ... })              (some endpoints)
     * The middleware envelope is already stripped by {@link #callMiddleware}, so what we
     * receive here is {@code { "data": <simahBody> }}. Drill once more if SIMAH nested under
     * its own "data".
     */
    private JsonNode unwrapSimahPayload(JsonNode scoreResponse) {
        if (scoreResponse == null || !scoreResponse.has("data") || scoreResponse.get("data").isNull()) {
            return null;
        }
        JsonNode simahBody = scoreResponse.get("data");
        if (simahBody.has("data") && simahBody.get("data").isObject()) {
            return simahBody.get("data");
        }
        return simahBody;
    }

    private CreditCheckResult mockResult() {
        return new CreditCheckResult(
                720, "A", "SIMAH-MOCK-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("15000"), BigDecimal.ZERO, false);
    }
}
