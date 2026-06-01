package com.ksa.financing.infra.audit;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emits a structured log line per API event using a dedicated logger
 * ("api-audit"). Logback routes this logger to the Logstash appender so
 * downstream pipelines can index into {@code api-audit-*} indexes without
 * mixing with application logs.
 */
public class ApiAuditLogger {

    private static final Logger LOG = LoggerFactory.getLogger("api-audit");

    public void log(ApiAuditEvent event) {
        if (event == null) return;
        Map<String, Object> kv = toMap(event);
        if (event.getStatus() == ApiAuditEvent.Status.SUCCESS) {
            LOG.info("api_audit", StructuredArguments.entries(kv));
        } else if (event.getStatus() == ApiAuditEvent.Status.SERVER_ERROR
                || event.getStatus() == ApiAuditEvent.Status.TIMEOUT
                || event.getStatus() == ApiAuditEvent.Status.CIRCUIT_OPEN) {
            LOG.error("api_audit", StructuredArguments.entries(kv));
        } else {
            LOG.warn("api_audit", StructuredArguments.entries(kv));
        }
    }

    private Map<String, Object> toMap(ApiAuditEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("audit_type", "api");
        m.put("direction", e.getDirection() == null ? null : e.getDirection().name().toLowerCase());
        m.put("service", e.getService());
        m.put("environment", e.getEnvironment());
        m.put("method", e.getMethod());
        m.put("path", e.getPath());
        m.put("query_string", e.getQueryString());
        m.put("full_url", e.getFullUrl());
        m.put("status_code", e.getStatusCode());
        m.put("status", e.getStatus() == null ? null : e.getStatus().name());
        m.put("latency_ms", e.getLatencyMs());
        m.put("request_size", e.getRequestSize());
        m.put("response_size", e.getResponseSize());
        m.put("request_headers", e.getRequestHeaders());
        m.put("response_headers", e.getResponseHeaders());
        m.put("request_body", e.getRequestBody());
        m.put("response_body", e.getResponseBody());
        m.put("user_id", e.getUserId());
        m.put("tenant_id", e.getTenantId());
        m.put("roles", e.getRoles());
        m.put("client_ip", e.getClientIp());
        m.put("user_agent", e.getUserAgent());
        m.put("correlation_id", e.getCorrelationId());
        m.put("trace_id", e.getTraceId());
        m.put("span_id", e.getSpanId());
        m.put("third_party_name", e.getThirdPartyName());
        m.put("third_party_api", e.getThirdPartyApi());
        m.put("error_code", e.getErrorCode());
        m.put("error_message", e.getErrorMessage());
        m.put("stack_trace", e.getStackTrace());
        // Business identifiers — emitted as a nested object so Elasticsearch
        // indexes each key (mobile, nationalId, customerId, loanId ...) as a
        // separate searchable field for journey filtering.
        if (e.getBusiness() != null && !e.getBusiness().isEmpty()) {
            m.put("business", e.getBusiness());
        }
        return m;
    }
}
