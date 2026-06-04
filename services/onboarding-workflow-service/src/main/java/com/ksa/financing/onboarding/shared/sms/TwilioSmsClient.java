package com.ksa.financing.onboarding.shared.sms;

import com.ksa.financing.onboarding.shared.keycloak.OnboardingKeycloakClient;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends SMS via Twilio <b>through the middleware-third-party service</b> (never directly).
 *
 * <p>The Twilio Account SID + Auth Token live in the middleware's
 * {@code api_environment_configs} (managed from the Third Party Management panel),
 * so this service holds no Twilio secrets. We only POST the message to
 * {@code /api/v1/execute/TWILIO_SEND_SMS}; the middleware applies BASIC auth,
 * form-urlencodes the body and forwards it to Twilio, returning the
 * {@code ExecuteApiResponse} envelope.</p>
 *
 * <p>The {@code From} (Twilio sender) number is the only non-secret value kept here,
 * from {@code twilio.from-number}. Disabled by default ({@code twilio.enabled=false}).</p>
 */
@Component
public class TwilioSmsClient {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsClient.class);

    private final RestTemplate restTemplate;
    private final OnboardingKeycloakClient keycloakClient;
    private final boolean enabled;
    private final String middlewareBaseUrl;
    private final String middlewareClientSecret;
    private final String apiCode;
    private final String fromNumber;

    public TwilioSmsClient(RestTemplate restTemplate,
                           OnboardingKeycloakClient keycloakClient,
                           @Value("${twilio.enabled:false}") boolean enabled,
                           @Value("${middleware.base-url:http://middleware-third-party:8093}") String middlewareBaseUrl,
                           @Value("${middleware.client-secret:ob-svc-mw-secret-2026-x9k4p}") String middlewareClientSecret,
                           @Value("${twilio.api-code:TWILIO_SEND_SMS}") String apiCode,
                           @Value("${twilio.from-number:}") String fromNumber) {
        this.restTemplate = restTemplate;
        this.keycloakClient = keycloakClient;
        this.enabled = enabled;
        this.middlewareBaseUrl = middlewareBaseUrl.replaceAll("/+$", "");
        this.middlewareClientSecret = middlewareClientSecret;
        this.apiCode = apiCode;
        this.fromNumber = fromNumber;
    }

    /** Outcome of an SMS send attempt. */
    public record SmsResult(boolean sent, String providerSid, String status, String errorMessage) {
        static SmsResult disabled() { return new SmsResult(false, null, "DISABLED", "Twilio SMS disabled (twilio.enabled=false)"); }
    }

    /**
     * Sends {@code body} to {@code toNumber} (E.164, e.g. {@code +9665XXXXXXXX}) via the
     * middleware Twilio provider. Never throws — failures are returned in {@link SmsResult}.
     */
    @SuppressWarnings("unchecked")
    public SmsResult send(String toNumber, String body) {
        if (!enabled) {
            log.info("[SMS DISABLED] would send to {} : {}", maskMobile(toNumber), body);
            return SmsResult.disabled();
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            return new SmsResult(false, null, "CONFIG_ERROR", "twilio.from-number not configured");
        }

        String url = middlewareBaseUrl + "/api/v1/execute/" + apiCode;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(keycloakClient.obtainAdminToken());
        headers.set("X-Secret-Key", middlewareClientSecret);
        headers.set("X-Caller-Service", "onboarding-workflow-service");
        headers.set("X-Mobile-Number", toNumber);

        // Twilio Messages API params — the middleware form-urlencodes these.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("To", toNumber);
        payload.put("From", fromNumber);
        payload.put("Body", body);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
            Map<String, Object> envelope = response.getBody();
            if (envelope == null) {
                return new SmsResult(false, null, "FAILED", "Empty response from middleware");
            }
            // Platform wraps every response in { data: {...}, message, timestamp } —
            // the real ExecuteApiResponse lives under "data".
            @SuppressWarnings("unchecked")
            Map<String, Object> data = envelope.get("data") instanceof Map<?, ?> dm
                    ? (Map<String, Object>) dm : envelope;

            boolean success = Boolean.TRUE.equals(data.get("success"));
            Map<String, Object> respBody = data.get("responseBody") instanceof Map<?, ?> rb
                    ? (Map<String, Object>) rb : null;
            String sid = respBody != null && respBody.get("sid") != null
                    ? String.valueOf(respBody.get("sid")) : null;
            String status = respBody != null && respBody.get("status") != null
                    ? String.valueOf(respBody.get("status")) : null;

            if (success) {
                log.info("Twilio SMS sent to {} via middleware (sid={}, status={})",
                        maskMobile(toNumber), sid, status);
                return new SmsResult(true, sid, status != null ? status : "sent", null);
            }

            // Surface the actual provider error: Twilio's code+message if present,
            // else the middleware's errorMessage.
            String err;
            if (respBody != null && respBody.get("message") != null) {
                Object code = respBody.get("code");
                err = (code != null ? code + ": " : "") + respBody.get("message");
            } else if (data.get("errorMessage") != null) {
                err = String.valueOf(data.get("errorMessage"));
            } else {
                err = "SMS send failed (HTTP " + data.get("httpStatus") + ")";
            }
            log.warn("Twilio SMS to {} failed via middleware: {}", maskMobile(toNumber), err);
            return new SmsResult(false, sid, "FAILED", err);
        } catch (Exception e) {
            log.error("Twilio SMS to {} failed (middleware call): {}", maskMobile(toNumber), e.getMessage());
            return new SmsResult(false, null, "FAILED", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
