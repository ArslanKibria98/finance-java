package com.ksa.financing.infra.audit;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable record of a single API interaction (inbound or outbound).
 * Serialized as a flat JSON document and shipped to Elasticsearch via Logstash.
 */
@Value
@Builder
public class ApiAuditEvent {

    public enum Direction { INBOUND, OUTBOUND }

    public enum Status {
        SUCCESS,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        BUSINESS_FAILURE,
        CLIENT_ERROR,
        SERVER_ERROR,
        TIMEOUT,
        CIRCUIT_OPEN
    }

    Instant timestamp;
    Direction direction;
    String service;
    String environment;

    String method;
    String path;
    String queryString;
    String fullUrl;

    Integer statusCode;
    Status status;
    Long latencyMs;

    Long requestSize;
    Long responseSize;
    Map<String, String> requestHeaders;
    Map<String, String> responseHeaders;
    String requestBody;
    String responseBody;

    String userId;
    String tenantId;
    String roles;
    String clientIp;
    String userAgent;

    String correlationId;
    String traceId;
    String spanId;

    String thirdPartyName;

    String errorCode;
    String errorMessage;
    String stackTrace;

    // Business identifiers — extracted from request/response body + URL path
    // so an entire customer journey can be reconstructed with a single filter.
    Map<String, String> business;

    public static Status statusFromHttpCode(int code) {
        if (code >= 200 && code < 300) return Status.SUCCESS;
        if (code == 401) return Status.UNAUTHORIZED;
        if (code == 403) return Status.FORBIDDEN;
        if (code == 404) return Status.NOT_FOUND;
        if (code == 422) return Status.BUSINESS_FAILURE;
        if (code >= 400 && code < 500) return Status.CLIENT_ERROR;
        if (code >= 500) return Status.SERVER_ERROR;
        return Status.SERVER_ERROR;
    }
}
