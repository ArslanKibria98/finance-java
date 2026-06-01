package com.ksa.financing.onboarding.shared.facia;

import com.ksa.financing.infra.audit.ApiAuditEvent;
import com.ksa.financing.infra.audit.ApiAuditLogger;
import com.ksa.financing.onboarding.shared.keycloak.OnboardingKeycloakClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST client for the Facia.ai KYC platform.
 *
 * <p>Phase 2C: calls Facia <b>through the middleware-third-party service</b> rather than
 * hitting Facia directly. The middleware owns the credentials, request/response audit
 * log, and cost tracking. We map our two operations to registered API codes:</p>
 * <ul>
 *   <li>{@code POST /api/v1/execute/FACIA_DOC_VERIFY/simple} — ID/Passport OCR + authenticity</li>
 *   <li>{@code POST /api/v1/execute/FACIA_FACE_MATCH/simple} — selfie vs document face 1:1 match</li>
 * </ul>
 *
 * <p>The middleware wraps Facia's actual response inside {@code ExecuteApiResponse}
 * ({@code requestId, httpStatus, responseBody, durationMs, success, errorMessage}).
 * This client unwraps {@code responseBody} before parsing the Facia payload exactly
 * the same way as before.</p>
 *
 * <p>Set {@code facia.mock=true} to bypass both the middleware and Facia, returning
 * canned responses for local smoke testing.</p>
 */
@Component
public class FaciaClient {

    private static final Logger log = LoggerFactory.getLogger(FaciaClient.class);

    private static final String API_CODE_DOC_VERIFY = "FACIA_DOC_VERIFY";
    private static final String API_CODE_FACE_MATCH = "FACIA_FACE_MATCH";

    private final RestTemplate restTemplate;
    private final OnboardingKeycloakClient keycloakClient;
    private final String middlewareBaseUrl;
    private final String middlewareClientSecret;
    private final boolean mock;
    private final ApiAuditLogger auditLogger;
    private final String serviceName;
    private final String environment;

    public FaciaClient(RestTemplate restTemplate,
                       OnboardingKeycloakClient keycloakClient,
                       ObjectProvider<ApiAuditLogger> auditLoggerProvider,
                       @Value("${spring.application.name:onboarding-workflow-service}") String serviceName,
                       @Value("${spring.profiles.active:dev}") String environment,
                       @Value("${middleware.base-url:http://middleware-third-party:8093}") String middlewareBaseUrl,
                       @Value("${middleware.client-secret:ob-svc-mw-secret-2026-x9k4p}") String middlewareClientSecret,
                       @Value("${facia.mock:false}") boolean mock) {
        this.restTemplate = restTemplate;
        this.keycloakClient = keycloakClient;
        this.auditLogger = auditLoggerProvider.getIfAvailable();
        this.serviceName = serviceName;
        this.environment = environment;
        this.middlewareBaseUrl = middlewareBaseUrl.replaceAll("/+$", "");
        this.middlewareClientSecret = middlewareClientSecret;
        this.mock = mock;
        if (mock) {
            log.warn("Facia client running in MOCK mode — document + face-match calls will return canned responses");
        }
    }

    /**
     * Submits a document image (ID or Passport) for OCR + authenticity check
     * via {@code FACIA_DOC_VERIFY} in middleware-third-party.
     */
    @SuppressWarnings("unchecked")
    public FaciaDocumentResult verifyDocument(String documentImageBase64) {
        return verifyDocument(documentImageBase64, FaciaContext.empty());
    }

    @SuppressWarnings("unchecked")
    public FaciaDocumentResult verifyDocument(String documentImageBase64, FaciaContext context) {
        if (mock) {
            FaciaDocumentResult r = mockDocumentResult();
            emitMockAudit(API_CODE_DOC_VERIFY, context,
                    "{\"mock\":true,\"event\":\"" + r.event() + "\",\"reference_id\":\"" + r.referenceId() + "\"}");
            return r;
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "document_verification");
        body.put("file", normaliseImage(documentImageBase64));

        try {
            Map<String, Object> faciaResult = callMiddleware(API_CODE_DOC_VERIFY, body, context);
            if (faciaResult == null) {
                log.warn("Facia document-verification returned null body via middleware");
                return new FaciaDocumentResult(false, null, null,
                        "Empty response from Facia", Map.of());
            }
            Map<String, Object> result = unwrapFaciaResult(faciaResult);
            String event = stringValue(result.get("event"));
            String referenceId = stringValue(result.get("reference_id"));
            String declineReason = stringValue(result.get("decline_reason"));
            boolean accepted = "verification_accepted".equalsIgnoreCase(event);

            // Real Facia shape nests OCR fields under `ocr_results` (with name as a
            // sub-object) and country / native-name / guardian under
            // `additional_data.proof`. Flatten both into a single extracted map for
            // the workflow / customer-service.
            Map<String, Object> extracted = new LinkedHashMap<>();
            Map<String, Object> ocr = asMap(result.get("ocr_results"));
            if (ocr != null) {
                Map<String, Object> name = asMap(ocr.get("name"));
                if (name != null) {
                    copyIfPresent(name, extracted,
                            "first_name", "middle_name", "last_name", "full_name");
                }
                copyIfPresent(ocr, extracted,
                        "dob", "document_number", "gender", "expiry_date", "issue_date",
                        "full_address", "age");
                Object selectedType = ocr.get("selected_type");
                if (selectedType instanceof java.util.List<?> list && !list.isEmpty()) {
                    extracted.put("selected_type", String.valueOf(list.get(0)));
                } else if (selectedType != null) {
                    extracted.put("selected_type", selectedType);
                }
            }
            Map<String, Object> proof = asMap(asMap(result.get("additional_data")) == null
                    ? null : asMap(result.get("additional_data")).get("proof"));
            if (proof != null) {
                if (proof.get("country_code") != null) extracted.put("nationality", proof.get("country_code"));
                copyIfPresent(proof, extracted,
                        "country", "country_native", "full_name_native",
                        "guardian_name", "document_official_name", "document_country");
                // Fall back to proof.* for any OCR field still missing (Facia
                // sometimes populates only one side).
                String[] proofFallback = {"dob", "expiry_date", "issue_date",
                        "document_number", "gender", "full_name"};
                for (String key : proofFallback) {
                    if (!extracted.containsKey(key) && proof.get(key) != null) {
                        extracted.put(key, proof.get(key));
                    }
                }
            }

            return new FaciaDocumentResult(accepted, referenceId, event, declineReason, extracted);
        } catch (Exception e) {
            log.error("Facia document-verification call failed via middleware: {}", e.getMessage(), e);
            return new FaciaDocumentResult(false, null, "error",
                    "Facia call failed: " + e.getMessage(), Map.of());
        }
    }

    /**
     * Performs a 1:1 face match between a selfie and the ID document face via
     * {@code FACIA_FACE_MATCH} in middleware-third-party.
     */
    @SuppressWarnings("unchecked")
    public FaciaFaceMatchResult faceMatch(String selfieBase64, String documentBase64) {
        return faceMatch(selfieBase64, documentBase64, FaciaContext.empty());
    }

    @SuppressWarnings("unchecked")
    public FaciaFaceMatchResult faceMatch(String selfieBase64, String documentBase64, FaciaContext context) {
        if (mock) {
            FaciaFaceMatchResult r = mockFaceMatchResult();
            emitMockAudit(API_CODE_FACE_MATCH, context,
                    "{\"mock\":true,\"similarity_status\":\"" + r.similarityStatus()
                            + "\",\"similarity_score\":" + r.similarityScore()
                            + ",\"reference_id\":\"" + r.referenceId() + "\"}");
            return r;
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "photo_id_match");
        body.put("face_frame", normaliseImage(selfieBase64));
        body.put("id_frame", normaliseImage(documentBase64));

        try {
            Map<String, Object> faciaResult = callMiddleware(API_CODE_FACE_MATCH, body, context);
            if (faciaResult == null) {
                return new FaciaFaceMatchResult(false, null, null, null,
                        "Empty response from Facia");
            }
            Map<String, Object> result = unwrapFaciaResult(faciaResult);
            String similarityStatus = stringValue(result.get("similarity_status"));
            Double similarityScore = parseDouble(result.get("similarity_score"));
            String referenceId = stringValue(result.get("reference_id"));
            boolean match = "1".equals(similarityStatus);
            return new FaciaFaceMatchResult(match, referenceId, similarityScore, similarityStatus, null);
        } catch (Exception e) {
            log.error("Facia face-match call failed via middleware: {}", e.getMessage(), e);
            return new FaciaFaceMatchResult(false, null, null, null,
                    "Facia call failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Middleware call helper
    // -------------------------------------------------------------------------

    /**
     * Posts {@code body} to {@code /api/v1/execute/{apiCode}/simple}, then strips
     * the middleware envelope and returns the Facia response body as a Map.
     *
     * <p>Business identifiers in {@code context} are forwarded to the middleware as
     * {@code X-Mobile-Number}, {@code X-National-Id}, {@code X-Customer-Id},
     * {@code X-Application-Id}, {@code X-Context-Type} and {@code X-Idempotency-Key}
     * headers so the audit log + persisted {@code client_request} row carry the
     * customer link for Kibana dashboards.</p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callMiddleware(String apiCode, Map<String, Object> body, FaciaContext context) {
        // External endpoint (not /simple) — uses an ONBOARDING_SERVICE DEV client so
        // middleware proxies to the real Facia.ai (TEST_MOCK_CLIENT would hit the
        // MockResponseDispatcher because its env=TEST).
        String url = middlewareBaseUrl + "/api/v1/execute/" + apiCode;
        // Middleware /execute/{apiCode} now requires JWT — fetch a service-account
        // token from Keycloak via client_credentials grant.
        String bearer = keycloakClient.obtainAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearer);
        headers.set("X-Secret-Key", middlewareClientSecret);
        headers.set("X-Caller-Service", "onboarding-workflow-service");
        if (context != null) {
            applyHeaderIfPresent(headers, "X-Mobile-Number", context.mobileNumber());
            applyHeaderIfPresent(headers, "X-National-Id", context.nationalId());
            applyHeaderIfPresent(headers, "X-Customer-Id", context.customerId());
            applyHeaderIfPresent(headers, "X-Application-Id", context.applicationId());
            applyHeaderIfPresent(headers, "X-Context-Type", context.contextType());
            applyHeaderIfPresent(headers, "X-Idempotency-Key", context.idempotencyKey());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        Map<String, Object> envelope = response.getBody();
        if (envelope == null) return null;

        // ExecuteApiResponse → { requestId, httpStatus, responseBody, durationMs, success, errorMessage }
        Boolean ok = (Boolean) envelope.get("success");
        if (ok != null && !ok) {
            log.warn("middleware reported Facia call failure: {}", envelope.get("errorMessage"));
        }
        Object responseBody = envelope.get("responseBody");
        if (responseBody instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        // Some middleware deployments wrap in `data` envelope before responseBody
        Object dataNode = envelope.get("data");
        if (dataNode instanceof Map<?, ?> data) {
            Object inner = ((Map<String, Object>) data).get("responseBody");
            if (inner instanceof Map<?, ?> innerMap) {
                return (Map<String, Object>) innerMap;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void applyHeaderIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private static String normaliseImage(String image) {
        if (image == null) return null;
        if (image.startsWith("data:image")) return image;
        return "data:image/png;base64," + image;
    }

    /** Facia payload is typically {@code { status, message, result: { data: { ... } } }}. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapFaciaResult(Map<String, Object> envelope) {
        Object resultNode = envelope.get("result");
        if (resultNode instanceof Map<?, ?> map) {
            Object dataNode = map.get("data");
            if (dataNode instanceof Map<?, ?> data) {
                return (Map<String, Object>) data;
            }
            return (Map<String, Object>) map;
        }
        return envelope;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private static void copyIfPresent(Map<String, Object> src, Map<String, Object> dst, String... keys) {
        for (String k : keys) {
            if (src.containsKey(k) && src.get(k) != null) {
                dst.put(k, src.get(k));
            }
        }
    }

    private static String stringValue(Object o) {
        return o == null ? null : o.toString();
    }

    private static Double parseDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Emits a synthetic OUTBOUND api-audit event for a mocked Facia call so that
     * mock-mode onboardings still surface in the third-party dashboards — filterable
     * by phone ({@code business.mobile}) and by {@code third_party_api}. Best-effort:
     * never throws into the onboarding flow.
     */
    private void emitMockAudit(String apiCode, FaciaContext context, String responseJson) {
        if (auditLogger == null) return;
        try {
            Map<String, String> business = new LinkedHashMap<>();
            if (context != null) {
                putIfPresent(business, "mobile", context.mobileNumber());
                putIfPresent(business, "nationalId", context.nationalId());
                putIfPresent(business, "customerId", context.customerId());
                putIfPresent(business, "applicationId", context.applicationId());
            }
            ApiAuditEvent event = ApiAuditEvent.builder()
                    .timestamp(Instant.now())
                    .direction(ApiAuditEvent.Direction.OUTBOUND)
                    .service(serviceName)
                    .environment(environment)
                    .method("POST")
                    .path("/api/v1/execute/" + apiCode)
                    .fullUrl("mock://facia/" + apiCode)
                    .statusCode(200)
                    .status(ApiAuditEvent.Status.SUCCESS)
                    .latencyMs(0L)
                    .responseBody(responseJson)
                    .thirdPartyName("facia")
                    .thirdPartyApi(apiCode)
                    .correlationId(MDC.get("correlationId"))
                    .traceId(MDC.get("traceId"))
                    .spanId(MDC.get("spanId"))
                    .tenantId(MDC.get("tenantId"))
                    .business(business.isEmpty() ? null : business)
                    .build();
            auditLogger.log(event);
            log.info("[MOCK] emitted synthetic Facia audit: api={} mobile={}", apiCode,
                    context != null ? context.mobileNumber() : null);
        } catch (Exception e) {
            log.debug("Failed to emit mock Facia audit event: {}", e.getMessage());
        }
    }

    private static void putIfPresent(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v);
    }

    // -------------------------------------------------------------------------
    // Mock responses (facia.mock=true)
    // -------------------------------------------------------------------------

    private FaciaDocumentResult mockDocumentResult() {
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String referenceId = "mock-doc-" + uniqueSuffix;
        // Unique 13-digit document number per run so repeated mock onboardings don't
        // collide on the customer national-ID uniqueness check (CUSTOMER.PROFILE.ALREADY_EXISTS).
        String documentNumber = String.valueOf(
                Math.abs(java.util.UUID.randomUUID().getMostSignificantBits() % 9_000_000_000_000L)
                        + 1_000_000_000_000L);
        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("full_name", "Arslan Kibria");
        extracted.put("dob", "1999-07-15");
        extracted.put("document_number", documentNumber);
        extracted.put("gender", "M");
        extracted.put("expiry_date", "2032-12-18");
        extracted.put("issue_date", "2022-12-18");
        extracted.put("age", 26);
        extracted.put("selected_type", "id_card");
        extracted.put("nationality", "PK");
        extracted.put("country", "pakistan");
        extracted.put("country_native", "اسلامی جمہوریہ پاکستان");
        extracted.put("full_name_native", "مرزا مومن بیگ");
        extracted.put("guardian_name", "Mushtaq Ahmed");
        extracted.put("document_official_name", "Pakistan National Identity Card");
        extracted.put("document_country", "Pakistan");
        log.info("[MOCK] Facia document-verification accepted: refId={}", referenceId);
        return new FaciaDocumentResult(true, referenceId, "verification_accepted", null, extracted);
    }

    private FaciaFaceMatchResult mockFaceMatchResult() {
        String referenceId = "mock-selfie-" + java.util.UUID.randomUUID();
        log.info("[MOCK] Facia face-match accepted: refId={} score=0.92", referenceId);
        return new FaciaFaceMatchResult(true, referenceId, 0.92, "1", null);
    }
}
