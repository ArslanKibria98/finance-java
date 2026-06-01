package com.ksa.financing.infra.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls business identifiers out of request/response bodies and URL paths so
 * an entire customer journey (onboarding → KYC → application → disbursement)
 * can be reconstructed by filtering Kibana on a single value (mobile, NID,
 * customerId, loanId, etc.).
 *
 * <p>The extractor is intentionally conservative — it only looks at common
 * field names and known path patterns. Anything it finds becomes a top-level
 * field on the {@link ApiAuditEvent} (prefixed with {@code business.*}).</p>
 */
@Slf4j
@RequiredArgsConstructor
public class BusinessContextExtractor {

    private final ObjectMapper objectMapper;

    /** Field synonyms grouped by canonical business identifier. */
    private static final Map<String, List<String>> FIELD_SYNONYMS = Map.ofEntries(
            Map.entry("mobile",            List.of("mobile", "mobilenumber", "phone", "phonenumber", "msisdn")),
            Map.entry("nationalId",        List.of("nationalid", "national_id", "nid", "iqama", "iqamanumber")),
            Map.entry("customerId",        List.of("customerid", "customer_id", "cif", "cifnumber", "globaluid")),
            Map.entry("loanId",            List.of("loanid", "loan_id", "loanaccountid")),
            Map.entry("loanNumber",        List.of("loannumber", "loan_number")),
            Map.entry("applicationId",     List.of("applicationid", "application_id", "loanapplicationid")),
            Map.entry("applicationNumber", List.of("applicationnumber", "application_number", "applicationno", "appnumber")),
            Map.entry("onboardingId",      List.of("onboardingid", "onboarding_id", "workflowid", "workflow_id")),
            Map.entry("walletId",          List.of("walletid", "wallet_id")),
            Map.entry("productId",         List.of("productid", "product_id", "productcode")),
            Map.entry("tenantId",          List.of("tenantid", "tenant_id")),
            Map.entry("email",             List.of("email", "emailaddress"))
    );

    /** Path patterns: regex → field name. */
    private static final List<PathPattern> PATH_PATTERNS = List.of(
            new PathPattern(Pattern.compile("/customers/([\\w-]+)"), "customerId"),
            new PathPattern(Pattern.compile("/cif/([\\w-]+)"), "customerId"),
            new PathPattern(Pattern.compile("/loans/([\\w-]+)"), "loanId"),
            new PathPattern(Pattern.compile("/applications/([\\w-]+)"), "applicationId"),
            new PathPattern(Pattern.compile("/onboarding/([\\w-]+)"), "onboardingId"),
            new PathPattern(Pattern.compile("/wallets/([\\w-]+)"), "walletId"),
            // KYC paths: only accept numeric IDs (NIDs are 10 digits in KSA)
            new PathPattern(Pattern.compile("/kyc/[^/]+/(\\d{8,15})"), "nationalId"),
            new PathPattern(Pattern.compile("/yakeen/lookup/(\\d{8,15})"), "nationalId"),
            new PathPattern(Pattern.compile("/simah/check/(\\d{8,15})"), "nationalId")
    );

    /**
     * Extract business context from path + raw (unmasked) request and response bodies.
     * Returns a flat map keyed by canonical names ("mobile", "nationalId", ...).
     */
    public Map<String, String> extract(String path, String rawRequestBody, String rawResponseBody) {
        return extract(path, null, rawRequestBody, rawResponseBody);
    }

    /**
     * Extract business context from path + headers + raw request/response bodies.
     * Headers are inspected for X-Mobile-Number, X-National-Id, X-Customer-Id so that
     * third-party adapters (Nafath, Tahaquq, etc.) which carry identifiers in headers
     * rather than body still get linked to the customer.
     */
    public Map<String, String> extract(String path, Map<String, String> headers,
                                       String rawRequestBody, String rawResponseBody) {
        Map<String, String> out = new LinkedHashMap<>();

        // 1. URL path patterns
        if (path != null) {
            for (PathPattern p : PATH_PATTERNS) {
                Matcher m = p.regex.matcher(path);
                if (m.find()) {
                    out.putIfAbsent(p.field, m.group(1));
                }
            }
        }

        // 2. Headers (third-party adapters propagate identifiers via X-* headers)
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                String hk = e.getKey().toLowerCase(Locale.ROOT);
                String hv = e.getValue().trim();
                if (hv.isEmpty()) continue;
                if (hk.equals("x-mobile-number") || hk.equals("x-mobile")) {
                    out.putIfAbsent("mobile", hv);
                } else if (hk.equals("x-national-id") || hk.equals("x-nid")) {
                    out.putIfAbsent("nationalId", hv);
                } else if (hk.equals("x-customer-id") || hk.equals("x-cif")) {
                    out.putIfAbsent("customerId", hv);
                } else if (hk.equals("x-tenant-id")) {
                    out.putIfAbsent("tenantId", hv);
                }
            }
        }

        // 3. Request body
        extractFromJson(rawRequestBody, out);

        // 4. Response body (often contains generated IDs)
        extractFromJson(rawResponseBody, out);

        return out;
    }

    private void extractFromJson(String body, Map<String, String> out) {
        if (body == null || body.isBlank()) return;
        try {
            JsonNode root = objectMapper.readTree(body);
            walk(root, out, 0);
        } catch (Exception ignore) {
            // Body wasn't JSON — silently skip
        }
    }

    private void walk(JsonNode node, Map<String, String> out, int depth) {
        if (node == null || depth > 6) return;
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                JsonNode value = entry.getValue();
                if (value.isValueNode()) {
                    String canonical = canonicalFor(key);
                    if (canonical != null) {
                        String v = value.asText();
                        if (v != null && !v.isBlank() && !"null".equalsIgnoreCase(v)) {
                            out.putIfAbsent(canonical, v);
                        }
                    }
                } else {
                    walk(value, out, depth + 1);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> walk(child, out, depth + 1));
        }
    }

    private String canonicalFor(String fieldNameLower) {
        for (Map.Entry<String, List<String>> e : FIELD_SYNONYMS.entrySet()) {
            for (String syn : e.getValue()) {
                if (fieldNameLower.equals(syn)) return e.getKey();
            }
        }
        return null;
    }

    @Getter
    private static final class PathPattern {
        final Pattern regex;
        final String field;
        PathPattern(Pattern regex, String field) {
            this.regex = regex; this.field = field;
        }
    }
}
