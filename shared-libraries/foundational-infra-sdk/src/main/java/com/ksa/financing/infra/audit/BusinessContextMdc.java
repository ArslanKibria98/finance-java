package com.ksa.financing.infra.audit;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request-scoped propagation of business identifiers (mobile, customerId,
 * nationalId, ...) via SLF4J {@link MDC}.
 *
 * <p>The inbound {@link ApiAuditFilter} seeds these from the originating request
 * so that every OUTBOUND call made while handling it — inter-service REST,
 * Keycloak, and especially third-party providers (Scotia, Twilio, Sullis, Nafath)
 * whose payloads do NOT carry the customer's mobile — still gets tagged with the
 * originating customer's identifiers. This lets a single Kibana filter
 * ({@code business.mobile: "<phone>"}) reconstruct the full journey including the
 * third-party hops.</p>
 *
 * <p>Keys are stored under the {@code biz.} prefix to keep them distinct from
 * {@code correlationId}/{@code traceId} and from the audit document's own
 * {@code business.*} fields.</p>
 */
public final class BusinessContextMdc {

    public static final String MDC_PREFIX = "biz.";

    private BusinessContextMdc() {
    }

    /** Put each extracted identifier into MDC (prefixed). Returns the MDC keys set, for cleanup. */
    public static Map<String, String> seed(Map<String, String> business) {
        Map<String, String> seeded = new LinkedHashMap<>();
        if (business == null) return seeded;
        business.forEach((k, v) -> {
            if (k == null || v == null || v.isBlank()) return;
            String mdcKey = MDC_PREFIX + k;
            MDC.put(mdcKey, v);
            seeded.put(mdcKey, v);
        });
        return seeded;
    }

    /** Read the request-scoped business identifiers back out of MDC (canonical keys, prefix stripped). */
    public static Map<String, String> current() {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        Map<String, String> out = new LinkedHashMap<>();
        if (ctx == null) return out;
        ctx.forEach((k, v) -> {
            if (k != null && v != null && k.startsWith(MDC_PREFIX) && !v.isBlank()) {
                out.put(k.substring(MDC_PREFIX.length()), v);
            }
        });
        return out;
    }

    /** Merge the request-scoped identifiers into an outbound event's business map (existing values win). */
    public static Map<String, String> mergeInto(Map<String, String> outboundBusiness) {
        Map<String, String> merged = outboundBusiness == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(outboundBusiness);
        current().forEach(merged::putIfAbsent);
        return merged;
    }

    /** Remove previously-seeded keys from MDC. */
    public static void clear(Map<String, String> seeded) {
        if (seeded == null) return;
        seeded.keySet().forEach(MDC::remove);
    }
}
