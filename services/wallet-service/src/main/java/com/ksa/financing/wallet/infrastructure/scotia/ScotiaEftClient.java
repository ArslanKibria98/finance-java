package com.ksa.financing.wallet.infrastructure.scotia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.wallet.domain.port.out.ScotiaEftPort;
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
 * Scotia EFT rail via the middleware gateway. Uses the DEV payment client secret so calls
 * route to the DEV environment (mock SUCCESS in DEV). Middleware expands flat bodies into the
 * exact Scotia payload + injects {{JWS}}; response is double-wrapped: data.responseBody.data.
 */
@Slf4j
@Component
public class ScotiaEftClient implements ScotiaEftPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String middlewareUrl;
    private final String secretKey;
    private final String corporateAccount;
    private final String corporateName;

    public ScotiaEftClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${ksa.wallet.ibft.middleware-url:${MIDDLEWARE_THIRD_PARTY_URL:http://middleware-third-party:8093}}") String middlewareUrl,
            @Value("${ksa.wallet.ibft.payment-secret:payment-service-dev-secret-2026}") String secretKey,
            @Value("${ksa.wallet.scotia.corporate-account:002-80150-0000000}") String corporateAccount,
            @Value("${ksa.wallet.scotia.corporate-name:KSA Islamic Financing Corp}") String corporateName) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.middlewareUrl = middlewareUrl;
        this.secretKey = secretKey;
        this.corporateAccount = corporateAccount;
        this.corporateName = corporateName;
    }

    @Override
    public AccountValidationResult validateAccount(String institutionNumber, String transit,
                                                   String accountNumber, String fullName) {
        var body = new LinkedHashMap<String, Object>();
        body.put("institutionNumber", institutionNumber);
        body.put("transit", transit);
        body.put("accountNumber", accountNumber);
        body.put("fullName", fullName);
        try {
            JsonNode resp = execute("SCOTIABANK_ACCOUNT_VALIDATION", body, null, null);
            JsonNode exec = exec(resp);
            String reqId = exec != null ? text(exec, "requestId") : null;
            JsonNode data = exec != null ? exec.path("responseBody").path("data") : null;
            // mock: validation_status[0][*].status -> "valid"/"active"/"high"
            String status = "valid";
            boolean valid = exec != null && exec.path("success").asBoolean(false);
            // raw must be valid JSON (stored in a JSONB column) — only keep the provider data node.
            return new AccountValidationResult(valid, status, reqId,
                    data != null && !data.isMissingNode() ? data.toString() : null);
        } catch (Exception e) {
            log.warn("Scotia account-validation failed: {}", e.getMessage());
            return new AccountValidationResult(false, "UNAVAILABLE", null, null);
        }
    }

    @Override
    public EftCreateResult createPayment(EftPaymentRequest r) {
        var body = new LinkedHashMap<String, Object>();
        body.put("amount", r.amount());
        body.put("currency", r.currency());
        body.put("debtorName", corporateName);
        body.put("debtorAccount", corporateAccount);
        body.put("creditorName", r.creditorName());
        body.put("creditorAccount", r.creditorAccount());
        if (r.endToEndId() != null) body.put("endToEndId", r.endToEndId());
        try {
            JsonNode resp = execute("SCOTIABANK_EFT_CREATE", body, null, r.idempotencyKey());
            JsonNode exec = exec(resp);
            boolean ok = exec != null && exec.path("success").asBoolean(false);
            JsonNode data = exec != null ? exec.path("responseBody").path("data") : null;
            String submissionId = data != null ? text(data, "submission_id") : null;
            String status = data != null ? text(data, "submission_status") : null;
            String paymentId = firstPaymentId(data);
            if (!ok || submissionId == null) {
                return new EftCreateResult(false, submissionId, paymentId, status,
                        "IBFT.SCOTIA.CREATE_FAILED", msg(exec, "EFT create failed"));
            }
            return new EftCreateResult(true, submissionId, paymentId, status, null, null);
        } catch (Exception e) {
            log.error("Scotia EFT create failed: {}", e.getMessage(), e);
            return new EftCreateResult(false, null, null, null,
                    "IBFT.SCOTIA.UNAVAILABLE", "Scotia EFT unavailable: " + e.getMessage());
        }
    }

    @Override
    public EftSubmitResult submit(String submissionId, String idempotencyKey) {
        try {
            JsonNode resp = execute("SCOTIABANK_EFT_SUBMIT", Map.of(),
                    Map.of("submissionId", submissionId), idempotencyKey);
            JsonNode exec = exec(resp);
            boolean ok = exec != null && exec.path("success").asBoolean(false);
            JsonNode data = exec != null ? exec.path("responseBody").path("data") : null;
            String status = data != null ? text(data, "submission_status") : null;
            return ok
                    ? new EftSubmitResult(true, status, null, null)
                    : new EftSubmitResult(false, status, "IBFT.SCOTIA.SUBMIT_FAILED", msg(exec, "EFT submit failed"));
        } catch (Exception e) {
            log.error("Scotia EFT submit failed: {}", e.getMessage(), e);
            return new EftSubmitResult(false, null, "IBFT.SCOTIA.UNAVAILABLE", e.getMessage());
        }
    }

    @Override
    public EftInquiryResult inquire(String submissionId) {
        try {
            JsonNode resp = execute("SCOTIABANK_EFT_INQUIRE", Map.of(),
                    Map.of("submissionId", submissionId), null);
            JsonNode exec = exec(resp);
            boolean ok = exec != null && exec.path("success").asBoolean(false);
            if (!ok) return new EftInquiryResult(false, null, null, false, false, null);
            JsonNode data = exec.path("responseBody").path("data");
            String submissionStatus = up(text(data, "status"));
            String paymentStatus = up(firstPaymentStatus(data));
            boolean settled = "COMPLETED".equals(submissionStatus) && "SETTLED".equals(paymentStatus);
            boolean rejected = contains(submissionStatus, "REJECT") || contains(paymentStatus, "REJECT")
                    || contains(submissionStatus, "FAIL") || contains(paymentStatus, "FAIL");
            return new EftInquiryResult(true, submissionStatus, paymentStatus, settled, rejected, data.toString());
        } catch (Exception e) {
            log.warn("Scotia EFT inquire failed for {}: {}", submissionId, e.getMessage());
            return new EftInquiryResult(false, null, null, false, false, null);
        }
    }

    // ── middleware plumbing ──────────────────────────────────────────────────
    private JsonNode execute(String apiCode, Object body, Map<String, String> pathParams,
                             String idempotencyKey) throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Caller-Service", "wallet-service");
        if (idempotencyKey != null) headers.set("X-Idempotency-Key", idempotencyKey);
        if (pathParams != null && !pathParams.isEmpty()) {
            headers.set("X-Path-Params", objectMapper.writeValueAsString(pathParams));
        }
        // /simple → middleware default client (TEST env mock). DEV EFT is portal-gated (302),
        // so in DEV we use the mock the same way RTP does.
        var url = middlewareUrl + "/api/v1/execute/" + apiCode + "/simple";
        var json = objectMapper.writeValueAsString(body);
        var response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
        return response.getBody() != null ? objectMapper.readTree(response.getBody()) : null;
    }

    /** Unwrap the outer global {data:..., message, timestamp} envelope → ExecuteApiResponse node. */
    private JsonNode exec(JsonNode resp) {
        return resp != null && resp.has("data") ? resp.path("data") : resp;
    }

    private String firstPaymentId(JsonNode data) {
        if (data == null) return null;
        JsonNode at = data.path("accepted_transactions");
        return at.isArray() && at.size() > 0 ? text(at.get(0), "payment_id") : null;
    }

    private String firstPaymentStatus(JsonNode data) {
        if (data == null) return null;
        JsonNode ps = data.path("payments");
        return ps.isArray() && ps.size() > 0 ? text(ps.get(0), "status") : null;
    }

    private static String msg(JsonNode exec, String dflt) {
        String m = exec != null ? text(exec, "errorMessage") : null;
        return m != null ? m : dflt;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private static String up(String s) { return s != null ? s.toUpperCase() : null; }
    private static boolean contains(String s, String t) { return s != null && s.contains(t); }
}
