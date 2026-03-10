package com.ksa.financing.onboarding.workflow.activity.impl;

import com.ksa.islamic.orchestration.activity.risk.AmlRiskScoringActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporal activity implementation that calls the Risk Service AML scoring endpoint via HTTP.
 *
 * <p>This is NOT a Spring bean — it is instantiated manually and registered
 * with the Temporal Worker on the RISK_ASSESSMENT_QUEUE. Dependencies are
 * provided via constructor injection.</p>
 *
 * <p>The Risk Service's {@code POST /api/v1/risk/aml-score} endpoint is public
 * (no JWT required). Tenant identification is via X-Tenant-Id header.</p>
 */
public class AmlRiskScoringActivityImpl implements AmlRiskScoringActivity {

    private static final Logger log = LoggerFactory.getLogger(AmlRiskScoringActivityImpl.class);

    private final RestTemplate restTemplate;
    private final String riskServiceUrl;

    public AmlRiskScoringActivityImpl(RestTemplate restTemplate, String riskServiceUrl) {
        this.restTemplate = restTemplate;
        this.riskServiceUrl = riskServiceUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AmlRiskScoringResult calculateAmlScore(AmlRiskScoringInput input) {
        log.info("Calling Risk Service AML scoring for customer={}, tenant={}",
                input.customerId(), input.tenantId());

        try {
            String url = riskServiceUrl + "/api/v1/risk/aml-score";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", input.tenantId());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("nationalIdHash", input.nationalIdHash());
            requestBody.put("nationality", input.nationality());
            requestBody.put("cityName", input.cityName());
            requestBody.put("occupationCode", input.occupationCode());
            requestBody.put("monthlyIncome", input.monthlyIncome());
            requestBody.put("sourceOfIncome", input.sourceOfIncome());
            requestBody.put("productRiskTier", input.productRiskTier());
            requestBody.put("isPep", input.isPep());
            requestBody.put("isOnInternalList", input.isOnInternalList());
            requestBody.put("customerId", input.customerId());
            requestBody.put("idempotencyKey", input.idempotencyKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return AmlRiskScoringResult.failure("Empty response from Risk Service");
            }

            // Risk Service wraps response in ApiResponse{data, message, timestamp}
            Map<String, Object> data = body.containsKey("data")
                    ? (Map<String, Object>) body.get("data") : body;

            if (data == null || !data.containsKey("assessmentId")) {
                return AmlRiskScoringResult.failure("Invalid response structure from Risk Service");
            }

            String assessmentId = data.get("assessmentId").toString();
            BigDecimal totalScore = new BigDecimal(data.get("totalScore").toString());
            String riskLevel = (String) data.get("riskLevel");
            boolean dominantOverride = Boolean.TRUE.equals(data.get("dominantOverride"));
            String dominantCategory = (String) data.get("dominantCategory");

            // Parse breakdown
            List<CategoryBreakdown> breakdown = new ArrayList<>();
            if (data.containsKey("breakdown") && data.get("breakdown") instanceof List<?> rawBreakdown) {
                for (Object item : rawBreakdown) {
                    if (item instanceof Map<?, ?> bMap) {
                        breakdown.add(new CategoryBreakdown(
                                (String) bMap.get("categoryCode"),
                                (String) bMap.get("categoryName"),
                                (String) bMap.get("matchedFactor"),
                                toBigDecimal(bMap.get("categoryWeight")),
                                toBigDecimal(bMap.get("factorWeightPct")),
                                toBigDecimal(bMap.get("rating"))
                        ));
                    }
                }
            }

            log.info("AML score received: assessmentId={}, totalScore={}, riskLevel={}, dominantOverride={}",
                    assessmentId, totalScore, riskLevel, dominantOverride);

            return new AmlRiskScoringResult(
                    assessmentId, totalScore, riskLevel,
                    dominantOverride, dominantCategory,
                    breakdown, true, null
            );

        } catch (Exception e) {
            log.error("AML risk scoring call failed: {}", e.getMessage(), e);
            return AmlRiskScoringResult.failure("Risk Service call failed: " + e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }
}
