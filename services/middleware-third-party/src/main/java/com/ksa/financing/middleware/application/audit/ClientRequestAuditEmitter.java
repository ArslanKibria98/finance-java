package com.ksa.financing.middleware.application.audit;

import com.ksa.financing.infra.audit.PiiMasker;
import com.ksa.financing.middleware.domain.model.ClientRequest;
import com.ksa.financing.middleware.domain.model.EnvironmentType;
import com.ksa.financing.middleware.domain.model.RequestStatus;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ships persisted {@code client_request_{test|dev|prod}} rows to ELK by emitting
 * one structured event per save through the dedicated {@code api-audit} logger.
 *
 * <p>Currently scoped to <b>Facia</b> calls only — the Customer-360 Kibana
 * dashboard needs the Facia mock/live responses cross-linked to a phone number
 * before any other provider. Non-Facia rows are ignored to keep the index
 * volume bounded; extend {@link #shouldEmit(ClientRequest)} as more providers
 * join the dashboard.</p>
 *
 * <p>The event reuses the {@code api-audit} logger so the existing Logstash
 * pipeline routes it to {@code api-audit-inbound-middleware-third-party-*}.
 * Dashboards query it via {@code audit_type:client_request} alongside
 * {@code business.mobile} / {@code business.customerId}.</p>
 */
@Slf4j
@Component
public class ClientRequestAuditEmitter {

    private static final Logger AUDIT = LoggerFactory.getLogger("api-audit");

    private final PiiMasker piiMasker;

    @Value("${spring.application.name:middleware-third-party}")
    private String serviceName;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    public ClientRequestAuditEmitter(PiiMasker piiMasker) {
        this.piiMasker = piiMasker;
    }

    public void emit(ClientRequest request) {
        if (request == null || !shouldEmit(request)) return;

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("@timestamp", Instant.now().toString());
        event.put("audit_type", "client_request");
        event.put("direction", "inbound");
        event.put("service", serviceName);
        event.put("environment", environment);
        event.put("client_request_env", request.getEnvironment() == null
                ? null : request.getEnvironment().name().toLowerCase());
        event.put("persisted_table", targetTable(request.getEnvironment()));
        event.put("provider_code", request.getProviderCode());
        event.put("api_code", request.getApiCode());
        event.put("client_request_id", request.getRequestId());
        event.put("http_method", request.getHttpMethod() == null ? null : request.getHttpMethod().name());
        event.put("path", request.getRequestUrl());
        event.put("full_url", request.getRequestUrl());
        event.put("status_code", request.getResponseStatus());
        event.put("status", mapStatus(request.getStatus()));
        event.put("latency_ms", request.getDurationMs());
        event.put("error_message", request.getErrorMessage());
        event.put("request_body", maskJson(request.getRequestBody()));
        event.put("response_body", maskJson(request.getResponseBody()));
        event.put("tenant_id", request.getTenantId() == null ? null : request.getTenantId().toString());
        event.put("caller_service", request.getCallerService());
        event.put("idempotency_key", request.getIdempotencyKey());
        event.put("api_cost", request.getApiCost());
        event.put("cost_currency", request.getCostCurrency());
        event.put("correlation_id", MDC.get("correlationId"));

        Map<String, String> business = new LinkedHashMap<>();
        putIfPresent(business, "mobile", request.getMobileNumber());
        putIfPresent(business, "nationalId", request.getNationalId());
        if (request.getCustomerId() != null) {
            business.put("customerId", request.getCustomerId().toString());
        }
        putIfPresent(business, "applicationId", request.getApplicationId());
        if (!business.isEmpty()) {
            event.put("business", business);
        }

        Integer status = request.getResponseStatus();
        if (status != null && status >= 500) {
            AUDIT.error("client_request_persisted", StructuredArguments.entries(event));
        } else if (status != null && status >= 400) {
            AUDIT.warn("client_request_persisted", StructuredArguments.entries(event));
        } else {
            AUDIT.info("client_request_persisted", StructuredArguments.entries(event));
        }
    }

    /** Only Facia for now — opt other providers in here as their dashboards land. */
    private boolean shouldEmit(ClientRequest request) {
        String provider = request.getProviderCode();
        return provider != null && provider.equalsIgnoreCase("FACIA");
    }

    private String targetTable(EnvironmentType env) {
        if (env == null) return null;
        return switch (env) {
            case TEST -> "client_request_test";
            case DEV -> "client_request_dev";
            case PROD -> "client_request_prod";
        };
    }

    private String mapStatus(RequestStatus s) {
        if (s == null) return null;
        return switch (s) {
            case SUCCESS -> "SUCCESS";
            case FAILED -> "BUSINESS_FAILURE";
            case TIMEOUT -> "TIMEOUT";
            default -> s.name();
        };
    }

    private String maskJson(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return piiMasker.truncate(piiMasker.maskBody(body, "application/json"));
        } catch (Exception e) {
            return null;
        }
    }

    private void putIfPresent(Map<String, String> out, String key, String value) {
        if (value != null && !value.isBlank()) out.put(key, value);
    }
}
