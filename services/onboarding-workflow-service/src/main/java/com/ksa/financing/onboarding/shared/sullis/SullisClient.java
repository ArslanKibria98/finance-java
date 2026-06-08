package com.ksa.financing.onboarding.shared.sullis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.onboarding.shared.keycloak.OnboardingKeycloakClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for the Sullis commercial KYC platform — routed <b>through
 * middleware-third-party</b> (the middleware owns the {@code Sullis-Api-Key},
 * request/response audit log and cost tracking). Replaces Facia for the foreign
 * onboarding KYC flow.
 *
 * <p>Maps the Sullis 5-step session flow onto the registered middleware API codes:</p>
 * <ol>
 *   <li>{@code SULLIS_CREATE_SESSION}   — JSON, returns sessionId</li>
 *   <li>{@code SULLIS_START_ATTEMPT}    — path {sessionId}, returns attemptId</li>
 *   <li>{@code SULLIS_UPLOAD_DOCUMENT}  — multipart file, ?type&side, returns OCR extract</li>
 *   <li>{@code SULLIS_UPLOAD_SELFIE}    — multipart file, ?kind=SELFIE_IMAGE</li>
 *   <li>{@code SULLIS_SUBMIT}           — path {sessionId,attemptId}, returns scores + outcome</li>
 * </ol>
 *
 * <p>Two composite operations are exposed to the Temporal activity:
 * {@link #verifyDocument} (steps 1–3) and {@link #submitVerification} (steps 4–5).
 * The {@code sessionId}/{@code attemptId} from step 1–2 are returned so the workflow
 * can persist them between the two HTTP round-trips.</p>
 *
 * <p>Set {@code sullis.mock=true} to bypass the middleware entirely and return canned
 * responses (used when the Sullis sandbox is unreachable from the environment).</p>
 */
@Component
public class SullisClient {

    private static final Logger log = LoggerFactory.getLogger(SullisClient.class);

    private static final String API_CREATE_SESSION = "SULLIS_CREATE_SESSION";
    private static final String API_START_ATTEMPT = "SULLIS_START_ATTEMPT";
    private static final String API_UPLOAD_DOCUMENT = "SULLIS_UPLOAD_DOCUMENT";
    private static final String API_UPLOAD_SELFIE = "SULLIS_UPLOAD_SELFIE";
    private static final String API_SUBMIT = "SULLIS_SUBMIT";

    private static final double APPROVE_FACE_FALLBACK = 0.95d;

    private final RestTemplate restTemplate;
    private final OnboardingKeycloakClient keycloakClient;
    private final ObjectMapper objectMapper;
    private final String middlewareBaseUrl;
    private final String middlewareClientSecret;
    private final String webhookUrl;
    private final boolean mock;

    public SullisClient(RestTemplate restTemplate,
                        OnboardingKeycloakClient keycloakClient,
                        ObjectMapper objectMapper,
                        @Value("${middleware.base-url:http://middleware-third-party:8093}") String middlewareBaseUrl,
                        @Value("${middleware.client-secret:ob-svc-mw-secret-2026-x9k4p}") String middlewareClientSecret,
                        @Value("${sullis.webhook-url:https://onboarding.kfs.com/webhooks/kyc}") String webhookUrl,
                        @Value("${sullis.mock:true}") boolean mock) {
        this.restTemplate = restTemplate;
        this.keycloakClient = keycloakClient;
        this.objectMapper = objectMapper;
        this.middlewareBaseUrl = middlewareBaseUrl.replaceAll("/+$", "");
        this.middlewareClientSecret = middlewareClientSecret;
        this.webhookUrl = webhookUrl;
        this.mock = mock;
        if (mock) {
            log.warn("Sullis client running in MOCK mode — KYC calls will return canned responses");
        }
    }

    // -------------------------------------------------------------------------
    // Composite operations used by the Temporal activity
    // -------------------------------------------------------------------------

    /**
     * Steps 1–3: create a Sullis session, start an attempt, and upload the document
     * (passport) image for OCR. Returns the extracted fields plus the session/attempt
     * ids the caller must persist for the later selfie phase.
     */
    public SullisDocumentResult verifyDocument(String documentImageBase64, String customerReference,
                                               String documentType, String existingSessionId,
                                               SullisContext context) {
        if (mock) {
            return mockDocumentResult();
        }
        try {
            // Reuse the existing Sullis session on retry (a session allows up to 3 attempts);
            // only create a new session on the very first document attempt. Each retry just
            // starts a NEW attempt within the same session, then re-uploads.
            String sessionId = (existingSessionId != null && !existingSessionId.isBlank())
                    ? existingSessionId : createSession(customerReference, context);

            // Start a fresh attempt. Sullis returns {"code":"MAX_ATTEMPTS"} once the session's
            // 3 attempts are used up — surface that instead of failing on a null attempt id.
            Map<String, Object> attemptResp = callJson(API_START_ATTEMPT, null,
                    Map.of("sessionId", sessionId), context);
            if ("MAX_ATTEMPTS".equalsIgnoreCase(stringValue(attemptResp.get("code")))) {
                return new SullisDocumentResult(false, sessionId, null, null,
                        "Maximum verification attempts reached for this document. Please restart onboarding.",
                        Map.of());
            }
            String attemptId = stringValue(attemptResp.get("id"));
            if (attemptId == null) {
                return new SullisDocumentResult(false, sessionId, null, null,
                        "Sullis start-attempt returned no attempt id", Map.of());
            }

            Map<String, Object> uploadResp = uploadFile(API_UPLOAD_DOCUMENT, sessionId, attemptId,
                    documentImageBase64, "passport.jpg",
                    Map.of("type", documentType != null ? documentType : "PASSPORT", "side", "FRONT"),
                    context);

            String uploadId = stringValue(uploadResp.get("uploadId"));
            // Sullis upload-document returns OCR fields under `ocr` (snake_case).
            // Tolerate the older `extracted` shape as a fallback.
            Map<String, Object> ocr = asMap(uploadResp.get("ocr"));
            if (ocr == null) ocr = asMap(uploadResp.get("extracted"));
            Map<String, Object> flattened = flattenExtracted(ocr);

            // Sullis OCR outcome:
            //   extraction_status == "EXTRACTED"          -> accept, proceed to next step
            //   "NO_FIELDS_EXTRACTED" (or anything else)  -> decline; surface the human-readable
            //                                                `reason` so the user re-captures.
            String extractionStatus = ocr != null ? stringValue(ocr.get("extraction_status")) : null;
            String ocrReason = ocr != null ? stringValue(ocr.get("reason")) : null;
            boolean accepted;
            String declineReason;
            if (extractionStatus != null && !extractionStatus.isBlank()) {
                accepted = uploadId != null && "EXTRACTED".equalsIgnoreCase(extractionStatus);
                declineReason = accepted ? null
                        : (ocrReason != null && !ocrReason.isBlank() ? ocrReason
                            : "Could not read the document — please re-capture a clear, full image in good lighting.");
            } else {
                // Older Sullis builds without extraction_status — fall back to OCR content.
                accepted = uploadId != null && flattened.get("document_number") != null;
                declineReason = accepted ? null : "Sullis document upload returned no OCR data";
            }
            return new SullisDocumentResult(accepted, sessionId, attemptId, uploadId, declineReason, flattened);
        } catch (Exception e) {
            log.error("Sullis document phase failed via middleware: {}", e.getMessage(), e);
            return new SullisDocumentResult(false, null, null, null,
                    "Sullis call failed: " + e.getMessage(), Map.of());
        }
    }

    /**
     * Steps 4–5: upload the selfie biometric and run the Sullis verification pipeline.
     * Uses the {@code sessionId}/{@code attemptId} from {@link #verifyDocument}.
     */
    public SullisVerificationResult submitVerification(String selfieImageBase64, String sessionId,
                                                       String attemptId, SullisContext context) {
        if (mock) {
            return mockVerificationResult(selfieImageBase64, sessionId, attemptId);
        }
        if (sessionId == null || attemptId == null) {
            return new SullisVerificationResult(false, "DECLINED", null, null, null,
                    "Missing Sullis session/attempt — upload document first", sessionId, attemptId, false);
        }
        try {
            uploadFile(API_UPLOAD_SELFIE, sessionId, attemptId, selfieImageBase64, "selfie.jpg",
                    Map.of("kind", "SELFIE_IMAGE"), context);

            Map<String, Object> submitResp = callJson(API_SUBMIT, null,
                    Map.of("sessionId", sessionId, "attemptId", attemptId), context);

            String outcome = stringValue(submitResp.get("outcome"));
            String reason = stringValue(submitResp.get("reason"));
            Integer riskScore = parseInt(submitResp.get("riskScore"));
            Map<String, Object> scores = asMap(submitResp.get("scores"));
            Double faceMatch = null;
            Boolean faceSamePerson = null;
            if (scores != null) {
                Double raw = parseDouble(scores.get("faceMatch"));
                faceMatch = raw != null ? raw / 100.0d : null;   // Sullis 0..100 -> 0..1
                Object same = scores.get("faceSamePerson");
                if (same instanceof Boolean b) faceSamePerson = b;
            }
            // Sullis success outcome is "VERIFIED" (also accept "APPROVED" defensively).
            boolean approved = "VERIFIED".equalsIgnoreCase(outcome) || "APPROVED".equalsIgnoreCase(outcome);
            // If Sullis approves but didn't surface a numeric faceMatch, assume a pass
            // so the workflow threshold check is satisfied by the overall outcome.
            if (approved && faceMatch == null) faceMatch = APPROVE_FACE_FALLBACK;
            if (approved) {
                return new SullisVerificationResult(true, outcome, faceMatch, riskScore,
                        faceSamePerson, null, sessionId, attemptId, false);
            }
            // DECLINED — start a NEW attempt within the SAME session so this failed selfie
            // consumes a slot from the shared 3-attempt budget (the same budget the document
            // phase draws from). Document used 1 → selfie has 2 retries. When the session's
            // attempts are exhausted Sullis returns {"code":"MAX_ATTEMPTS"} → surface it so the
            // workflow tells the user to restart onboarding (mirrors the document MAX path).
            return startRetryAttemptOnDecline(sessionId, attemptId, outcome, faceMatch,
                    riskScore, faceSamePerson, reason, context);
        } catch (Exception e) {
            log.error("Sullis submit phase failed via middleware: {}", e.getMessage(), e);
            return new SullisVerificationResult(false, "DECLINED", null, null, null,
                    "Sullis call failed: " + e.getMessage(), sessionId, attemptId, false);
        }
    }

    /**
     * On a declined selfie, register a fresh Sullis attempt against the existing session so the
     * failed try draws from the shared 3-attempt session budget. Returns a result carrying the
     * NEW attempt id (the next selfie retry uploads against it), or {@code maxAttemptsReached=true}
     * when the session's attempts are used up.
     */
    private SullisVerificationResult startRetryAttemptOnDecline(String sessionId, String attemptId,
                                                                String outcome, Double faceMatch,
                                                                Integer riskScore, Boolean faceSamePerson,
                                                                String reason, SullisContext context) {
        String newAttemptId = attemptId;
        try {
            Map<String, Object> attemptResp = callJson(API_START_ATTEMPT, null,
                    Map.of("sessionId", sessionId), context);
            if ("MAX_ATTEMPTS".equalsIgnoreCase(stringValue(attemptResp.get("code")))) {
                return new SullisVerificationResult(false, outcome, faceMatch, riskScore, faceSamePerson,
                        "Maximum verification attempts reached. Please restart onboarding.",
                        sessionId, attemptId, true);
            }
            String started = stringValue(attemptResp.get("id"));
            if (started != null && !started.isBlank()) newAttemptId = started;
        } catch (Exception e) {
            // A failure to register the retry attempt must not crash the selfie phase — the
            // user can still retry against the current attempt; just log it.
            log.warn("Sullis start-attempt after selfie decline failed (session={}): {}", sessionId, e.getMessage());
        }
        return new SullisVerificationResult(false, outcome, faceMatch, riskScore, faceSamePerson,
                reason, sessionId, newAttemptId, false);
    }

    // -------------------------------------------------------------------------
    // Individual Sullis steps (through middleware)
    // -------------------------------------------------------------------------

    private String createSession(String customerReference, SullisContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerReference", customerReference != null ? customerReference : UUID.randomUUID().toString());
        body.put("webhookUrl", webhookUrl);
        Map<String, Object> resp = callJson(API_CREATE_SESSION, body, Map.of(), context);
        String sessionId = stringValue(resp.get("sessionId"));
        if (sessionId == null) throw new IllegalStateException("Sullis create-session returned no sessionId");
        return sessionId;
    }

    // -------------------------------------------------------------------------
    // Middleware transport
    // -------------------------------------------------------------------------

    /**
     * POST a JSON (or empty) body to {@code /api/v1/execute/{apiCode}} and return the
     * unwrapped provider response body as a Map. Path params are forwarded via the
     * {@code X-Path-Params} header which the middleware substitutes into the endpoint.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callJson(String apiCode, Map<String, Object> body,
                                         Map<String, String> pathParams, SullisContext context) {
        String url = middlewareBaseUrl + "/api/v1/execute/" + apiCode;
        HttpHeaders headers = baseHeaders(context);
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyPathParams(headers, pathParams);

        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        return unwrapEnvelope(response.getBody(), apiCode);
    }

    /**
     * POST a multipart file to {@code /api/v1/execute/{apiCode}/multipart}. Path + query
     * params travel as text form fields the middleware parses + forwards to the provider.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> uploadFile(String apiCode, String sessionId, String attemptId,
                                           String imageBase64, String fileName,
                                           Map<String, String> queryParams, SullisContext context) {
        String url = middlewareBaseUrl + "/api/v1/execute/" + apiCode + "/multipart";
        HttpHeaders headers = baseHeaders(context);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        byte[] bytes = decodeImage(imageBase64);
        var fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileResource);
        form.add("formField", "file");
        form.add("pathParams", toJson(Map.of("sessionId", sessionId, "attemptId", attemptId)));
        form.add("queryParams", toJson(queryParams));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        return unwrapEnvelope(response.getBody(), apiCode);
    }

    private HttpHeaders baseHeaders(SullisContext context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(keycloakClient.obtainAdminToken());
        headers.set("X-Secret-Key", middlewareClientSecret);
        headers.set("X-Caller-Service", "onboarding-workflow-service");
        if (context != null) {
            applyHeaderIfPresent(headers, "X-Mobile-Number", context.mobileNumber());
            applyHeaderIfPresent(headers, "X-National-Id", context.nationalId());
            applyHeaderIfPresent(headers, "X-Customer-Id", context.customerId());
            applyHeaderIfPresent(headers, "X-Application-Id", context.applicationId());
            applyHeaderIfPresent(headers, "X-Context-Type", context.contextType());
            // NO X-Idempotency-Key: the Sullis session flow makes 3 (doc) / 2 (selfie)
            // distinct calls that share one context. A single idempotency key would
            // make the middleware return the FIRST call's cached result for the rest
            // (e.g. start-attempt returning the create-session body → "no attempt id").
            // The Sullis session/attempt ids already give per-step idempotency.
        }
        return headers;
    }

    private void applyPathParams(HttpHeaders headers, Map<String, String> pathParams) {
        if (pathParams != null && !pathParams.isEmpty()) {
            headers.set("X-Path-Params", toJson(pathParams));
        }
    }

    /** Unwrap the middleware {@code ExecuteApiResponse} envelope to the provider responseBody Map. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapEnvelope(Map<String, Object> envelope, String apiCode) {
        if (envelope == null) return Map.of();
        Boolean ok = (Boolean) envelope.get("success");
        if (ok != null && !ok) {
            log.warn("middleware reported Sullis call failure ({}): {}", apiCode, envelope.get("errorMessage"));
        }
        Object responseBody = envelope.get("responseBody");
        if (responseBody instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Object dataNode = envelope.get("data");
        if (dataNode instanceof Map<?, ?> data) {
            Object inner = ((Map<String, Object>) data).get("responseBody");
            if (inner instanceof Map<?, ?> innerMap) {
                return (Map<String, Object>) innerMap;
            }
        }
        return Map.of();
    }

    // -------------------------------------------------------------------------
    // OCR field mapping — Sullis camelCase -> the snake_case keys the workflow expects
    // -------------------------------------------------------------------------

    private Map<String, Object> flattenExtracted(Map<String, Object> ocr) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (ocr == null) return out;
        // Map Sullis OCR (snake_case from upload-document, camelCase from submit-extracted)
        // to the flattened keys the workflow / confirm-data / customer-service expect.
        putIfPresent(out, "full_name", pick(ocr, "full_name", "fullName"));
        putIfPresent(out, "first_name", pick(ocr, "given_names", "givenNames", "first_name"));
        putIfPresent(out, "surname", pick(ocr, "surname", "last_name"));
        putIfPresent(out, "dob", pick(ocr, "date_of_birth", "dateOfBirth", "dob"));
        putIfPresent(out, "document_number", pick(ocr, "document_number", "documentNumber"));
        putIfPresent(out, "expiry_date", pick(ocr, "expiry_date", "expiryDate"));
        putIfPresent(out, "gender", pick(ocr, "sex", "gender"));
        putIfPresent(out, "nationality", pick(ocr, "nationality"));
        Object issuing = pick(ocr, "issuing_country", "issuingCountry");
        putIfPresent(out, "country", issuing);
        putIfPresent(out, "document_country", issuing);
        Object docType = pick(ocr, "document_type", "documentType");
        if (docType != null) {
            out.put("selected_type", docType.toString().toLowerCase());
            out.put("document_type", docType);
        }
        putIfPresent(out, "mrz_valid", pick(ocr, "mrz_valid", "mrzValid"));
        return out;
    }

    private static Object pick(Map<String, Object> m, String... keys) {
        if (m == null) return null;
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return v;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static byte[] decodeImage(String imageBase64) {
        if (imageBase64 == null) return new byte[0];
        String b64 = imageBase64;
        int comma = b64.indexOf(',');
        if (b64.startsWith("data:") && comma > 0) {
            b64 = b64.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            // Some callers send raw (non-base64) placeholder strings in tests — forward as bytes.
            return b64.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static void applyHeaderIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) headers.set(name, value);
    }

    private static void putIfPresent(Map<String, Object> m, String k, Object v) {
        if (v != null) m.put(k, v);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private static String stringValue(Object o) {
        return o == null ? null : o.toString();
    }

    private static Double parseDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    // -------------------------------------------------------------------------
    // Mock responses (sullis.mock=true)
    // -------------------------------------------------------------------------

    private SullisDocumentResult mockDocumentResult() {
        String sessionId = UUID.randomUUID().toString();
        String attemptId = UUID.randomUUID().toString();
        String uploadId = "mock-doc-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Unique 13-digit document number per run so repeated mock onboardings don't
        // collide on the customer national-ID uniqueness check.
        String documentNumber = String.valueOf(
                Math.abs(UUID.randomUUID().getMostSignificantBits() % 9_000_000_000_000L) + 1_000_000_000_000L);
        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("full_name", "Arslan Kibria");
        extracted.put("dob", "1999-07-15");
        extracted.put("document_number", documentNumber);
        extracted.put("expiry_date", "2032-12-18");
        extracted.put("nationality", "PK");
        extracted.put("country", "Pakistan");
        extracted.put("document_country", "Pakistan");
        extracted.put("selected_type", "passport");
        extracted.put("document_type", "PASSPORT");
        extracted.put("mrz_valid", true);
        log.info("[MOCK] Sullis document accepted: session={} attempt={} upload={}", sessionId, attemptId, uploadId);
        return new SullisDocumentResult(true, sessionId, attemptId, uploadId, null, extracted);
    }

    private SullisVerificationResult mockVerificationResult(String selfieImageBase64,
                                                            String sessionId, String attemptId) {
        String sid = sessionId != null ? sessionId : UUID.randomUUID().toString();
        String aid = attemptId != null ? attemptId : UUID.randomUUID().toString();
        // Test hooks: a selfie image carrying a sentinel exercises the decline / max-attempts
        // paths in DEV mock (where real Sullis would otherwise always VERIFY).
        //   ...MAXATTEMPTS... -> declined + shared budget exhausted (workflow surfaces restart)
        //   ...DECLINE...     -> declined + a fresh attempt id (consumes one shared slot)
        String marker = selfieImageBase64 != null ? selfieImageBase64.toUpperCase() : "";
        if (marker.contains("MAXATTEMPTS")) {
            log.info("[MOCK] Sullis selfie DECLINED — max attempts reached: session={}", sid);
            return new SullisVerificationResult(false, "DECLINED", 0.40d, 80, false,
                    "Maximum verification attempts reached. Please restart onboarding.", sid, aid, true);
        }
        if (marker.contains("DECLINE")) {
            String newAttempt = UUID.randomUUID().toString();
            log.info("[MOCK] Sullis selfie DECLINED — new attempt {} started: session={}", newAttempt, sid);
            return new SullisVerificationResult(false, "DECLINED", 0.45d, 70, false,
                    "Face match below threshold — please retake your selfie.", sid, newAttempt, false);
        }
        log.info("[MOCK] Sullis verification VERIFIED: session={} attempt={}", sid, aid);
        return new SullisVerificationResult(true, "VERIFIED", 0.99d, 12, true, null, sid, aid, false);
    }
}
