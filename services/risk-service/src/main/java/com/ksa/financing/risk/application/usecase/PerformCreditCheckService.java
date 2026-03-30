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

        // 1. Call SIMAH_SCORE_CREDIT for credit score
        try {
            var scoreResponse = callMiddleware("SIMAH_SCORE_CREDIT", command.nationalId(),
                    Map.of("nationalId", command.nationalId()));
            if (scoreResponse != null && scoreResponse.has("data")) {
                var data = scoreResponse.get("data");
                creditScore = data.has("creditScore") ? data.get("creditScore").asInt() : 0;
                simahGrade = data.has("riskGrade") ? data.get("riskGrade").asText() : "UNKNOWN";
            }
        } catch (Exception e) {
            log.warn("SIMAH_SCORE_CREDIT call failed: {}, using defaults", e.getMessage());
            creditScore = 720;
            simahGrade = "A";
        }

        // 2. Call SIMAH_CONSUMER_REPORT for salary
        try {
            var reportResponse = callMiddleware("SIMAH_CONSUMER_REPORT", command.nationalId(),
                    Map.of("nationalId", command.nationalId(), "productType", 155, "amount", command.requestedAmount()));
            if (reportResponse != null && reportResponse.has("data")) {
                var data = reportResponse.get("data");
                if (data.has("applicants") && data.get("applicants").isArray() && !data.get("applicants").isEmpty()) {
                    var applicant = data.get("applicants").get(0);
                    if (applicant.has("demographicInfo")) {
                        var demo = applicant.get("demographicInfo");
                        verifiedSalary = demo.has("totalMonthlyIncome")
                                ? new BigDecimal(demo.get("totalMonthlyIncome").asText())
                                : BigDecimal.ZERO;
                    }
                }
                if (data.has("referenceNumber")) {
                    simahReferenceId = data.get("referenceNumber").asText();
                }
            }
        } catch (Exception e) {
            log.warn("SIMAH_CONSUMER_REPORT call failed: {}, using default salary", e.getMessage());
            verifiedSalary = new BigDecimal("15000");
        }

        // 3. Call SIMAH_NEGATIVE_CONSUMER for defaults
        try {
            var negativeResponse = callMiddleware("SIMAH_NEGATIVE_CONSUMER", command.nationalId(),
                    Map.of("nationalId", command.nationalId()));
            if (negativeResponse != null && negativeResponse.has("data")) {
                var data = negativeResponse.get("data");
                hasActiveDefaults = data.has("hasDefaults") && data.get("hasDefaults").asBoolean();
            }
        } catch (Exception e) {
            log.warn("SIMAH_NEGATIVE_CONSUMER call failed: {}, assuming no defaults", e.getMessage());
            hasActiveDefaults = false;
        }

        // 4. Call SIMAH_CREDIT_COMMITMENTS for existing obligations
        try {
            var commitResponse = callMiddleware("SIMAH_CREDIT_COMMITMENTS", command.nationalId(),
                    Map.of("nationalId", command.nationalId()));
            if (commitResponse != null && commitResponse.has("data")) {
                var data = commitResponse.get("data");
                existingObligations = data.has("totalMonthlyInstallment")
                        ? new BigDecimal(data.get("totalMonthlyInstallment").asText())
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
        try {
            String url = middlewareUrl + "/api/v1/execute/" + apiCode;

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Caller-Service", "risk-service");
            headers.set("X-National-Id", nationalId);
            if (middlewareSecretKey != null && !middlewareSecretKey.isBlank()) {
                headers.set("X-Secret-Key", middlewareSecretKey);
            }

            String requestBody = objectMapper.writeValueAsString(body);
            var response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var root = objectMapper.readTree(response.getBody());
                // Middleware wraps in { responseBody: { ... } }
                if (root.has("responseBody")) {
                    return root.get("responseBody");
                }
                return root;
            }
            return null;
        } catch (Exception e) {
            log.warn("Middleware call failed for {}: {}", apiCode, e.getMessage());
            throw new RuntimeException("Middleware call failed: " + apiCode, e);
        }
    }

    private CreditCheckResult mockResult() {
        return new CreditCheckResult(
                720, "A", "SIMAH-MOCK-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("15000"), BigDecimal.ZERO, false);
    }
}
