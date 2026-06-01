package com.ksa.financing.onboarding.workflow.activity.impl;

import com.ksa.islamic.orchestration.activity.risk.GeneralScoringActivity;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Temporal activity that calls risk-service's GENERAL credit scoring endpoint
 * after each onboarding stage. Persists a per-stage snapshot and updates
 * the customer's current cached score.
 *
 * <p>Not a Spring bean; instantiated by {@code TemporalWorkerConfig} and
 * registered on the {@code RISK_ASSESSMENT_QUEUE}. Calls the internal
 * service-to-service endpoint (no JWT; X-Tenant-Id header).
 */
public class GeneralScoringActivityImpl implements GeneralScoringActivity {

    private static final Logger log = LoggerFactory.getLogger(GeneralScoringActivityImpl.class);

    private final RestTemplate restTemplate;
    private final String riskServiceUrl;

    public GeneralScoringActivityImpl(RestTemplate restTemplate, String riskServiceUrl) {
        this.restTemplate = restTemplate;
        this.riskServiceUrl = riskServiceUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GeneralScoringResult scoreIncremental(GeneralScoringInput input) {
        log.info("[general-scoring] Calling risk-service stage={} customer={} answers={}",
                input.stage(), input.customerId(),
                input.answers() != null ? input.answers().size() : 0);

        try {
            String url = riskServiceUrl + "/internal/credit-scoring/general/evaluate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", input.tenantId());

            Map<String, Object> body = new HashMap<>();
            body.put("customerId", input.customerId());
            body.put("workflowId", input.workflowId());
            body.put("stage", input.stage());
            body.put("answers", input.answers() == null ? Map.of() : input.answers());
            body.put("persistSnapshot", true);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, req,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> raw = resp.getBody();
            if (raw == null) {
                return new GeneralScoringResult("INSUFFICIENT_DATA",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        "EMPTY_RESPONSE", 0, 0, "Empty response from risk-service");
            }

            Map<String, Object> data = raw.containsKey("data") && raw.get("data") instanceof Map<?, ?>
                    ? (Map<String, Object>) raw.get("data") : raw;

            String decision = strOrNull(data.get("decision"));
            BigDecimal scorePct = toBigDecimal(data.get("scorePercentage"));
            BigDecimal totalScore = toBigDecimal(data.get("totalScore"));
            BigDecimal maxScore = toBigDecimal(data.get("maxPossibleScore"));
            String reason = strOrNull(data.get("reasonCode"));
            int matched = toInt(data.get("matchedCriteria"));
            int total = toInt(data.get("totalCriteria"));
            String summary = strOrNull(data.get("summary"));

            log.info("[general-scoring] stage={} decision={} score={}% matched={}/{}",
                    input.stage(), decision, scorePct, matched, total);

            return new GeneralScoringResult(
                    decision, scorePct, totalScore, maxScore,
                    reason, matched, total, summary
            );

        } catch (Exception e) {
            log.warn("[general-scoring] call failed for stage={}: {} (continuing onboarding)",
                    input.stage(), e.getMessage());
            return new GeneralScoringResult("INSUFFICIENT_DATA",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "REMOTE_FAILURE", 0, 0,
                    "General scoring call failed: " + e.getMessage());
        }
    }

    private static String strOrNull(Object v) {
        return v == null ? null : v.toString();
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static int toInt(Object v) {
        if (v == null) return 0;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
