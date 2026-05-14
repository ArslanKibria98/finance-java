package com.ksa.financing.infra.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebClient ExchangeFilter that emits an {@link ApiAuditEvent} per outbound
 * reactive call. Used by services that talk to third parties (Nafath, Yakeen,
 * Simah, Keycloak, etc.) via WebClient.
 *
 * <p>Body capture intentionally omitted on the reactive path to avoid
 * consuming the stream twice; status / latency / URL / headers are enough
 * for dashboards and forensic investigation.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ApiAuditWebClientFilter implements ExchangeFilterFunction {

    private final ApiAuditProperties properties;
    private final ApiAuditLogger auditLogger;
    private final PiiMasker piiMasker;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        long start = System.currentTimeMillis();
        Map<String, String> reqHeaders = piiMasker.maskHeaders(flatten(request.headers()));

        return next.exchange(request)
                .doOnNext(resp -> emitSuccess(request, resp, reqHeaders, System.currentTimeMillis() - start))
                .doOnError(err -> emitError(request, err, reqHeaders, System.currentTimeMillis() - start));
    }

    private void emitSuccess(ClientRequest req, ClientResponse resp,
                             Map<String, String> reqHeaders, long latencyMs) {
        try {
            int code = resp.statusCode().value();
            URI uri = req.url();
            ApiAuditEvent event = ApiAuditEvent.builder()
                    .timestamp(Instant.now())
                    .direction(ApiAuditEvent.Direction.OUTBOUND)
                    .service(serviceName)
                    .environment(environment)
                    .method(req.method().name())
                    .path(uri.getPath())
                    .queryString(uri.getQuery())
                    .fullUrl(uri.toString())
                    .statusCode(code)
                    .status(ApiAuditEvent.statusFromHttpCode(code))
                    .latencyMs(latencyMs)
                    .requestHeaders(reqHeaders)
                    .responseHeaders(piiMasker.maskHeaders(flatten(resp.headers().asHttpHeaders())))
                    .correlationId(MDC.get("correlationId"))
                    .traceId(MDC.get("traceId"))
                    .spanId(MDC.get("spanId"))
                    .tenantId(MDC.get("tenantId"))
                    .thirdPartyName(resolveThirdParty(uri.getHost()))
                    .build();
            auditLogger.log(event);
        } catch (Exception e) {
            log.warn("WebClient audit emit failed: {}", e.getMessage());
        }
    }

    private void emitError(ClientRequest req, Throwable err,
                           Map<String, String> reqHeaders, long latencyMs) {
        try {
            URI uri = req.url();
            String simpleName = err.getClass().getSimpleName().toLowerCase();
            boolean timeout = simpleName.contains("timeout");
            ApiAuditEvent event = ApiAuditEvent.builder()
                    .timestamp(Instant.now())
                    .direction(ApiAuditEvent.Direction.OUTBOUND)
                    .service(serviceName)
                    .environment(environment)
                    .method(req.method().name())
                    .path(uri.getPath())
                    .queryString(uri.getQuery())
                    .fullUrl(uri.toString())
                    .statusCode(timeout ? 0 : 599)
                    .status(timeout ? ApiAuditEvent.Status.TIMEOUT : ApiAuditEvent.Status.SERVER_ERROR)
                    .latencyMs(latencyMs)
                    .requestHeaders(reqHeaders)
                    .correlationId(MDC.get("correlationId"))
                    .traceId(MDC.get("traceId"))
                    .spanId(MDC.get("spanId"))
                    .tenantId(MDC.get("tenantId"))
                    .thirdPartyName(resolveThirdParty(uri.getHost()))
                    .errorMessage(err.getMessage())
                    .build();
            auditLogger.log(event);
        } catch (Exception e) {
            log.warn("WebClient audit emit failed (error path): {}", e.getMessage());
        }
    }

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
}
