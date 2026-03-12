package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.FraudHistoryCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls fraud-service via REST to check fraud history.
 * Uses circuit breaker with fallback — if fraud-service is down, returns PASS
 * so that onboarding is not blocked (defense-in-depth, not a hard gate).
 */
@Component
@Slf4j
public class FraudHistoryCheckImpl implements FraudHistoryCheck {

    private final RestTemplate restTemplate;
    private final String fraudServiceUrl;

    public FraudHistoryCheckImpl(
            RestTemplate restTemplate,
            @Value("${app.services.fraud-service-url:http://localhost:8092}") String fraudServiceUrl) {
        this.restTemplate = restTemplate;
        this.fraudServiceUrl = fraudServiceUrl;
    }

    @Override
    public FraudHistoryResult check(FraudHistoryInput input) {
        log.info("Starting fraud history check via fraud-service");

        try {
            var request = Map.of(
                    "nationalIdHash", input.nidHash() != null ? input.nidHash() : "",
                    "mobileHash", input.mobileHash() != null ? input.mobileHash() : "",
                    "customerId", input.customerId() != null ? input.customerId() : ""
            );

            @SuppressWarnings("unchecked")
            var response = restTemplate.postForObject(
                    fraudServiceUrl + "/api/v1/fraud/history-check",
                    request,
                    Map.class
            );

            if (response == null) {
                log.warn("Fraud history check: null response from fraud-service, defaulting to PASS");
                return new FraudHistoryResult(CheckDecision.PASS, false, false, 0, null);
            }

            boolean hasFraudHistory = Boolean.TRUE.equals(response.get("hasFraudHistory"));
            int confirmedFraudCount = response.get("confirmedFraudCount") instanceof Number n ? n.intValue() : 0;
            int suspectedFraudCount = response.get("suspectedFraudCount") instanceof Number n ? n.intValue() : 0;
            int totalEvaluations = response.get("totalEvaluations") instanceof Number n ? n.intValue() : 0;
            String recommendation = (String) response.get("recommendation");

            if ("BLOCK".equals(recommendation)) {
                log.warn("CONFIRMED FRAUD detected via fraud-service: {} confirmed incident(s)", confirmedFraudCount);
                return new FraudHistoryResult(
                        CheckDecision.HARD_BLOCK, true, false, totalEvaluations, null
                );
            }

            if ("REVIEW_MANUAL".equals(recommendation)) {
                log.warn("SUSPECTED FRAUD detected via fraud-service: {} suspected incident(s)", suspectedFraudCount);
                return new FraudHistoryResult(
                        CheckDecision.FLAG_HIGH_RISK, false, true, totalEvaluations, null
                );
            }

            log.info("Fraud history check passed via fraud-service: hasFraudHistory={} total={}", hasFraudHistory, totalEvaluations);
            return new FraudHistoryResult(CheckDecision.PASS, false, false, totalEvaluations, null);

        } catch (Exception e) {
            log.warn("Fraud history check failed (fraud-service unavailable), defaulting to PASS: {}", e.getMessage());
            return new FraudHistoryResult(CheckDecision.PASS, false, false, 0, "Fraud-service unavailable - check skipped");
        }
    }
}
