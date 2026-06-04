package com.ksa.financing.wallet.infrastructure.scotia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.wallet.domain.port.out.ScotiaRtpPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls Scotiabank RTP APIs through the middleware-third-party gateway.
 *
 * Flow: payment-options inquiry (eligibility) -> commit (money movement).
 * Uses the internal {@code /api/v1/execute/{apiCode}/simple} endpoint (no secret key —
 * routed through the middleware default client). The middleware expands our flat body into
 * Scotia's exact RTP payload via the stored {@code request_template}.
 *
 * Zero hardcoding: URLs, api codes and the platform corporate account all come from config.
 */
@Slf4j
@Component
public class ScotiaRtpClient implements ScotiaRtpPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String middlewareUrl;
    private final String optionsApiCode;
    private final String commitApiCode;
    private final boolean optionsInquiryEnabled;

    public ScotiaRtpClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${ksa.wallet.scotia.rtp.middleware-url:http://middleware-third-party:8093}") String middlewareUrl,
            @Value("${ksa.wallet.scotia.rtp.options-api-code:SCOTIABANK_PAYMENT_OPTIONS_INQUIRY}") String optionsApiCode,
            @Value("${ksa.wallet.scotia.rtp.commit-api-code:SCOTIABANK_PAYMENT_COMMIT}") String commitApiCode,
            @Value("${ksa.wallet.scotia.rtp.options-inquiry-enabled:true}") boolean optionsInquiryEnabled) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.middlewareUrl = middlewareUrl;
        this.optionsApiCode = optionsApiCode;
        this.commitApiCode = commitApiCode;
        this.optionsInquiryEnabled = optionsInquiryEnabled;
    }

    @Override
    public RtpResult sendPayment(RtpPaymentRequest request) {
        // 1. Eligibility / options inquiry (best-effort — a reject here is informational in DEV mock)
        if (optionsInquiryEnabled) {
            try {
                execute(optionsApiCode, optionsBody(request), request.idempotencyKey() + "-opt");
            } catch (Exception ex) {
                log.warn("Scotia RTP options-inquiry failed (continuing to commit): {}", ex.getMessage());
            }
        }

        // 2. Commit the payment (the actual money movement)
        JsonNode response;
        try {
            response = execute(commitApiCode, commitBody(request), request.idempotencyKey());
        } catch (Exception ex) {
            log.error("Scotia RTP commit call failed: {}", ex.getMessage(), ex);
            return new RtpResult(false, null, null, null,
                    "SCOTIA.RTP.UNAVAILABLE", "Scotia RTP gateway unavailable: " + ex.getMessage());
        }

        // The middleware wraps the ExecuteApiResponse in a global {data:..., message, timestamp}
        // envelope, and the provider payload sits under responseBody.data.
        JsonNode exec = response != null && response.has("data") ? response.path("data") : response;
        boolean middlewareOk = exec != null && exec.path("success").asBoolean(false);
        JsonNode data = exec != null ? exec.path("responseBody").path("data") : null;
        String paymentId = data != null ? text(data, "payment_id") : null;
        String clearingRef = data != null ? text(data, "clearing_system_reference") : null;
        String status = data != null ? text(data, "status") : null;

        boolean success = middlewareOk && (status == null || isAccepted(status));
        if (!success) {
            String msg = response != null ? text(response, "errorMessage") : null;
            return new RtpResult(false, paymentId, clearingRef, status,
                    "SCOTIA.RTP.REJECTED", msg != null ? msg : "Scotia RTP rejected the payment");
        }
        return new RtpResult(true, paymentId, clearingRef, status, null, null);
    }

    private boolean isAccepted(String status) {
        String s = status.toUpperCase();
        return s.contains("SUCCESS") || s.contains("ACCEPT") || s.contains("COMPLETE") || s.contains("PENDING");
    }

    private JsonNode execute(String apiCode, Map<String, Object> body, String idempotencyKey) throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Caller-Service", "wallet-service");
        if (idempotencyKey != null) {
            headers.set("X-Idempotency-Key", idempotencyKey);
        }
        var url = middlewareUrl + "/api/v1/execute/" + apiCode + "/simple";
        var json = objectMapper.writeValueAsString(body);
        var response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
        return response.getBody() != null ? objectMapper.readTree(response.getBody()) : null;
    }

    private Map<String, Object> optionsBody(RtpPaymentRequest r) {
        var body = new LinkedHashMap<String, Object>();
        body.put("amount", r.amount());
        body.put("currency", r.currency());
        body.put("creditorAccount", r.creditorAccount());
        if (r.creditorEmail() != null) body.put("creditorEmail", r.creditorEmail());
        return body;
    }

    private Map<String, Object> commitBody(RtpPaymentRequest r) {
        var body = new LinkedHashMap<String, Object>();
        body.put("amount", r.amount());
        body.put("currency", r.currency());
        body.put("messageIdentification", r.messageIdentification());
        body.put("debtorName", r.debtorName());
        body.put("debtorAccount", r.debtorAccount());
        body.put("creditorName", r.creditorName());
        body.put("creditorAccount", r.creditorAccount());
        if (r.creditorEmail() != null) body.put("creditorEmail", r.creditorEmail());
        return body;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }
}
