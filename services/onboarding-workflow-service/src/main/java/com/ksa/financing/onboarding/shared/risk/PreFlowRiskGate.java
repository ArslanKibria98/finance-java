package com.ksa.financing.onboarding.shared.risk;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-flow risk + fraud gate used by Canada and Foreign onboarding controllers
 * (KSA has its own controller-embedded copy).
 *
 * <p>Calls risk-service {@code POST /api/v1/risk/internal-checks} with whatever
 * identifiers the caller has (mobile only for Canada/Foreign initiate — NID is
 * not collected until document upload). Forwards device fingerprint, IP,
 * session and tenant headers so the velocity rules can fire.</p>
 *
 * <p>Fail-closed: on any exception or non-PASS decision the gate returns a
 * {@link Decision} with {@code allowed=false} and the caller must NOT start
 * the workflow.</p>
 */
@Component
public class PreFlowRiskGate {

    private static final Logger log = LoggerFactory.getLogger(PreFlowRiskGate.class);

    private final RestTemplate restTemplate;
    private final String riskServiceUrl;

    public PreFlowRiskGate(RestTemplate restTemplate,
                           @Value("${app.services.risk-service-url}") String riskServiceUrl) {
        this.restTemplate = restTemplate;
        this.riskServiceUrl = riskServiceUrl;
    }

    /**
     * Run the pre-flow gate. Pass null for nationalId when the flow does not
     * collect it at initiate time (Canada / Foreign).
     */
    @SuppressWarnings("unchecked")
    public Decision check(String nationalId,
                          String mobileNumber,
                          String tenantId,
                          DeviceInfo deviceInfo,
                          HttpServletRequest httpRequest) {

        String url = riskServiceUrl + "/api/v1/risk/internal-checks";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId);
            if (deviceInfo != null) {
                if (deviceInfo.deviceId() != null) headers.set("X-Device-Id", deviceInfo.deviceId());
                if (deviceInfo.ipAddress() != null) headers.set("X-Client-Ip", deviceInfo.ipAddress());
            }
            if (httpRequest != null) {
                if (httpRequest.getHeader("X-Device-Fingerprint") != null) {
                    headers.set("X-Device-Fingerprint", httpRequest.getHeader("X-Device-Fingerprint"));
                }
                if (httpRequest.getHeader("X-Session-Id") != null) {
                    headers.set("X-Session-Id", httpRequest.getHeader("X-Session-Id"));
                }
            }

            Map<String, Object> body = new HashMap<>();
            if (nationalId != null && !nationalId.isBlank()) body.put("nationalId", nationalId);
            if (mobileNumber != null && !mobileNumber.isBlank()) body.put("mobileNumber", mobileNumber);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (response.getBody() == null) {
                log.warn("Risk-service returned null body — failing closed");
                return Decision.block("HARD_BLOCK", "Service temporarily unavailable. Please try again later.", null, null);
            }

            Map<String, Object> data = response.getBody();
            if (data.containsKey("data") && data.get("data") instanceof Map) {
                data = (Map<String, Object>) data.get("data");
            }

            String decision = data.get("overallDecision") != null ? data.get("overallDecision").toString() : null;
            String blockReason = (String) data.get("blockReason");
            String routeTo = (String) data.get("routeTo");
            String assessmentId = (String) data.get("assessmentId");

            log.info("Pre-flow risk gate: assessmentId={}, decision={}", assessmentId, decision);

            if ("PASS".equals(decision) || "ROUTE_ONBOARD".equals(decision) || "ROUTE_REGISTER".equals(decision)) {
                return Decision.allow(assessmentId);
            }
            if ("ROUTE_LOGIN".equals(decision)) {
                return Decision.block("ROUTE_LOGIN",
                        blockReason != null ? blockReason
                                : "An account already exists. Please log in.",
                        "LOGIN", assessmentId);
            }
            if ("ROUTE_REONBOARDING".equals(decision)) {
                return Decision.block("ROUTE_REONBOARDING",
                        "Welcome back! Your account is inactive. Please reactivate.",
                        "WELCOME_BACK", assessmentId);
            }
            // HARD_BLOCK + everything else
            return Decision.block(decision != null ? decision : "HARD_BLOCK",
                    blockReason != null ? blockReason : "Unable to proceed with registration at this time.",
                    routeTo, assessmentId);

        } catch (Exception e) {
            log.error("Pre-flow risk gate call failed — failing closed: {}", e.getMessage());
            return Decision.block("HARD_BLOCK",
                    "Service temporarily unavailable. Please try again later.", null, null);
        }
    }

    public record Decision(boolean allowed,
                           String status,
                           String message,
                           String routeTo,
                           String assessmentId) {
        static Decision allow(String assessmentId) {
            return new Decision(true, "PASS", null, null, assessmentId);
        }
        static Decision block(String status, String message, String routeTo, String assessmentId) {
            return new Decision(false, status, message, routeTo, assessmentId);
        }
    }
}
