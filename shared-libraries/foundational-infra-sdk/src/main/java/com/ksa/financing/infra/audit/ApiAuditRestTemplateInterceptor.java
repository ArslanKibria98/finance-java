package com.ksa.financing.infra.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Captures every outbound HTTP call made via {@link org.springframework.web.client.RestTemplate}
 * — inter-service REST, Keycloak admin calls, third-party providers (Nafath, Simah,
 * Fineract, etc.). Emits one {@link ApiAuditEvent} with {@code direction=OUTBOUND}.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiAuditRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final ApiAuditProperties properties;
    private final ApiAuditLogger auditLogger;
    private final PiiMasker piiMasker;
    private final BusinessContextExtractor businessExtractor;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // Propagate the correlation id downstream so the callee's inbound filter
        // reuses it instead of minting a new one — without this, a caller's outbound
        // error and the callee's real error response live under different
        // correlation_ids and cannot be linked in Kibana.
        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()
                && !request.getHeaders().containsKey("X-Correlation-ID")) {
            try {
                request.getHeaders().add("X-Correlation-ID", correlationId);
            } catch (UnsupportedOperationException ignore) {
                // headers already committed — nothing we can do, audit still records it
            }
        }

        long start = System.currentTimeMillis();
        Throwable failure = null;
        ClientHttpResponse response = null;
        byte[] respBytes = new byte[0];
        try {
            response = execution.execute(request, body);
            respBytes = response.getBody().readAllBytes();
            // wrap so caller still sees a readable stream
            response = new BufferedClientResponse(response, respBytes);
            return response;
        } catch (IOException | RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - start;
            try {
                emit(request, body, response, respBytes, latency, failure);
            } catch (Exception logEx) {
                log.warn("Failed to emit outbound audit event: {}", logEx.getMessage());
            }
        }
    }

    private void emit(HttpRequest request, byte[] body, ClientHttpResponse response,
                      byte[] respBytes, long latency, Throwable failure) throws IOException {

        URI uri = request.getURI();
        int statusCode = failure != null
                ? (isTimeout(failure) ? 0 : 599)
                : response.getStatusCode().value();

        ApiAuditEvent.Status status;
        if (failure != null) {
            status = isTimeout(failure) ? ApiAuditEvent.Status.TIMEOUT : ApiAuditEvent.Status.SERVER_ERROR;
        } else {
            status = ApiAuditEvent.statusFromHttpCode(statusCode);
        }

        String reqContentType = headerOrNull(request.getHeaders(), HttpHeaders.CONTENT_TYPE);
        String respContentType = response != null ? headerOrNull(response.getHeaders(), HttpHeaders.CONTENT_TYPE) : null;

        String rawReqBody = null;
        String reqBody = null;
        if (properties.isCaptureOutboundBody() && body != null && body.length > 0) {
            rawReqBody = new String(body, StandardCharsets.UTF_8);
            reqBody = piiMasker.truncate(piiMasker.maskBody(rawReqBody, reqContentType));
        }
        String rawRespBody = null;
        String respBody = null;
        if (properties.isCaptureOutboundBody() && respBytes.length > 0) {
            rawRespBody = new String(respBytes, StandardCharsets.UTF_8);
            respBody = piiMasker.truncate(piiMasker.maskBody(rawRespBody, respContentType));
        }

        java.util.Map<String, String> business = businessExtractor.extract(
                uri.getPath(), flatten(request.getHeaders()), rawReqBody, rawRespBody);

        ApiAuditEvent event = ApiAuditEvent.builder()
                .timestamp(Instant.now())
                .direction(ApiAuditEvent.Direction.OUTBOUND)
                .service(serviceName)
                .environment(environment)
                .method(request.getMethod().name())
                .path(uri.getPath())
                .queryString(uri.getQuery())
                .fullUrl(uri.toString())
                .statusCode(statusCode)
                .status(status)
                .latencyMs(latency)
                .requestSize(body == null ? 0L : (long) body.length)
                .responseSize((long) respBytes.length)
                .requestHeaders(piiMasker.maskHeaders(flatten(request.getHeaders())))
                .responseHeaders(response != null ? piiMasker.maskHeaders(flatten(response.getHeaders())) : null)
                .requestBody(reqBody)
                .responseBody(respBody)
                .correlationId(MDC.get("correlationId"))
                .traceId(MDC.get("traceId"))
                .spanId(MDC.get("spanId"))
                .tenantId(MDC.get("tenantId"))
                .thirdPartyName(resolveThirdParty(uri.getHost()))
                .thirdPartyApi(resolveThirdPartyApi(uri.getPath()))
                .errorMessage(failure != null ? failure.getMessage() : null)
                .business(business.isEmpty() ? null : business)
                .build();

        auditLogger.log(event);
    }

    private boolean isTimeout(Throwable t) {
        String name = t.getClass().getSimpleName().toLowerCase();
        if (name.contains("timeout")) return true;
        Throwable cause = t.getCause();
        return cause != null && cause.getClass().getSimpleName().toLowerCase().contains("timeout");
    }

    /**
     * Middleware-third-party routes every provider call through
     * {@code /api/v1/execute/{API_CODE}} (e.g. FACIA_DOC_VERIFY, NAFATH_INITIATE).
     * Surfacing that code as {@code third_party_api} lets one dashboard filter cover
     * both the middleware-side audit and the caller-side outbound audit.
     */
    private String resolveThirdPartyApi(String path) {
        if (path == null) return null;
        Matcher m = EXECUTE_PATH.matcher(path);
        return m.find() ? m.group(1) : null;
    }

    private static final java.util.regex.Pattern EXECUTE_PATH =
            java.util.regex.Pattern.compile("/execute/([A-Za-z0-9_]+)");

    private String resolveThirdParty(String host) {
        if (host == null) return null;
        String lower = host.toLowerCase();
        for (ApiAuditProperties.ThirdPartyMapping m : properties.getThirdPartyMappings()) {
            if (m.getHostContains() != null && lower.contains(m.getHostContains().toLowerCase())) {
                return m.getName();
            }
        }
        return null;
    }

    private Map<String, String> flatten(HttpHeaders headers) {
        Map<String, String> out = new LinkedHashMap<>();
        headers.forEach((k, v) -> out.put(k, String.join(",", v)));
        return out;
    }

    private String headerOrNull(HttpHeaders headers, String name) {
        List<String> v = headers.get(name);
        return (v == null || v.isEmpty()) ? null : v.get(0);
    }

    /**
     * Wrap the original response so the body stream can be read again
     * by the actual caller after we have consumed it for auditing.
     */
    private static final class BufferedClientResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] body;

        BufferedClientResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override public org.springframework.http.HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public void close() { delegate.close(); }
        @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
        @Override public HttpHeaders getHeaders() { return delegate.getHeaders(); }
    }
}
