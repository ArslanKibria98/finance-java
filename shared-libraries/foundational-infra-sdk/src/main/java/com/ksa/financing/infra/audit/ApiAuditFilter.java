package com.ksa.financing.infra.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Captures every inbound HTTP request/response and emits a single
 * {@link ApiAuditEvent} per call via {@link ApiAuditLogger}.
 *
 * <p>Runs after Spring Security so the JWT principal is already on the
 * SecurityContext (user id, tenant id, roles), and after Casbin so the
 * 403 path is also captured. Body capture uses Spring's
 * ContentCachingRequest/ResponseWrapper to avoid consuming the stream.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ApiAuditFilter extends OncePerRequestFilter implements Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiAuditProperties properties;
    private final ApiAuditLogger auditLogger;
    private final PiiMasker piiMasker;
    private final BusinessContextExtractor businessExtractor;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Override
    public int getOrder() {
        // Run BEFORE Spring Security (which sits around order -205 by default)
        // so we can audit requests that security itself rejects (401/403). The
        // JWT principal is still recovered via ApiAuditPrincipalInterceptor
        // (which fires inside DispatcherServlet, after security has populated
        // the SecurityContext).
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) return true;
        String path = request.getRequestURI();
        for (String pattern : properties.getExcludePaths()) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        HttpServletRequest wrappedReq = wrapRequest(request);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);

        Throwable failure = null;
        try {
            filterChain.doFilter(wrappedReq, wrappedResp);
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            long latency = System.currentTimeMillis() - start;
            try {
                emit(wrappedReq, wrappedResp, latency, failure);
            } catch (Exception logEx) {
                log.warn("Failed to emit api audit event: {}", logEx.getMessage());
            }
            // CRITICAL: copy buffered body to actual response stream
            wrappedResp.copyBodyToResponse();
        }
    }

    /**
     * If the request has a small enough body, eagerly read it into a
     * {@link CachedBodyRequestWrapper} so the audit can record it even when
     * Spring Security rejects the call before any controller reads the stream.
     * For large or unknown-length bodies, fall back to
     * {@link ContentCachingRequestWrapper} (which only caches bytes that the
     * application actually reads).
     */
    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        if (request instanceof CachedBodyRequestWrapper c) return c;
        if (request instanceof ContentCachingRequestWrapper w) return w;
        int contentLength = request.getContentLength();
        int cap = properties.getMaxBodySizeBytes();
        if (properties.isCaptureRequestBody() && contentLength >= 0 && contentLength <= cap) {
            try {
                return new CachedBodyRequestWrapper(request);
            } catch (IOException e) {
                log.debug("Could not eagerly cache request body: {}", e.getMessage());
            }
        }
        return new ContentCachingRequestWrapper(request);
    }

    private void emit(HttpServletRequest req,
                      ContentCachingResponseWrapper resp,
                      long latencyMs,
                      Throwable failure) {

        int statusCode = failure != null ? 500 : resp.getStatus();
        ApiAuditEvent.Status status = ApiAuditEvent.statusFromHttpCode(statusCode);

        byte[] reqBytes = extractRequestBytes(req);
        String rawReqBody = null;
        String reqBody = null;
        if (properties.isCaptureRequestBody() && reqBytes.length > 0) {
            rawReqBody = new String(reqBytes, charsetOrDefault(req.getCharacterEncoding()));
            reqBody = piiMasker.truncate(piiMasker.maskBody(rawReqBody, req.getContentType()));
        }

        String rawRespBody = null;
        String respBody = null;
        if (properties.isCaptureResponseBody()) {
            byte[] content = resp.getContentAsByteArray();
            if (content.length > 0) {
                rawRespBody = new String(content, charsetOrDefault(resp.getCharacterEncoding()));
                respBody = piiMasker.truncate(piiMasker.maskBody(rawRespBody, resp.getContentType()));
            }
        }

        // Extract business context (mobile, nationalId, customerId, loanId, ...)
        // from headers + RAW bodies before masking — masked values would be unusable for journey tracking.
        Map<String, String> reqHeaders = new java.util.LinkedHashMap<>();
        java.util.Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String n = headerNames.nextElement();
            reqHeaders.put(n, req.getHeader(n));
        }
        Map<String, String> business = businessExtractor.extract(req.getRequestURI(), reqHeaders, rawReqBody, rawRespBody);

        JwtPrincipal principal = resolvePrincipal(req);

        ApiAuditEvent event = ApiAuditEvent.builder()
                .timestamp(Instant.now())
                .direction(ApiAuditEvent.Direction.INBOUND)
                .service(serviceName)
                .environment(environment)
                .method(req.getMethod())
                .path(req.getRequestURI())
                .queryString(req.getQueryString())
                .fullUrl(buildFullUrl(req))
                .statusCode(statusCode)
                .status(status)
                .latencyMs(latencyMs)
                .requestSize((long) reqBytes.length)
                .responseSize((long) resp.getContentAsByteArray().length)
                .requestHeaders(piiMasker.maskHeaders(extractRequestHeaders(req)))
                .responseHeaders(piiMasker.maskHeaders(extractResponseHeaders(resp)))
                .requestBody(reqBody)
                .responseBody(respBody)
                .userId(principal.userId)
                .tenantId(principal.tenantId)
                .roles(principal.roles)
                .clientIp(extractClientIp(req))
                .userAgent(req.getHeader("User-Agent"))
                .correlationId(MDC.get("correlationId"))
                .traceId(MDC.get("traceId"))
                .spanId(MDC.get("spanId"))
                .errorMessage(failure != null ? failure.getMessage() : null)
                .stackTrace(failure != null ? compactStackTrace(failure) : null)
                .business(business.isEmpty() ? null : business)
                .build();

        auditLogger.log(event);
    }

    private byte[] extractRequestBytes(HttpServletRequest req) {
        if (req instanceof CachedBodyRequestWrapper c) {
            return c.getCachedBody();
        }
        if (req instanceof ContentCachingRequestWrapper w) {
            return w.getContentAsByteArray();
        }
        return new byte[0];
    }

    private Map<String, String> extractRequestHeaders(HttpServletRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String n = names.nextElement();
            headers.put(n, req.getHeader(n));
        }
        return headers;
    }

    private Map<String, String> extractResponseHeaders(HttpServletResponse resp) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String n : resp.getHeaderNames()) {
            headers.put(n, resp.getHeader(n));
        }
        return headers;
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }

    private String buildFullUrl(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder(req.getRequestURL());
        if (req.getQueryString() != null) {
            sb.append('?').append(req.getQueryString());
        }
        return sb.toString();
    }

    private java.nio.charset.Charset charsetOrDefault(String enc) {
        if (enc == null || enc.isBlank()) return StandardCharsets.UTF_8;
        try {
            return java.nio.charset.Charset.forName(enc);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String compactStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        String full = sw.toString();
        return full.length() > 4_000 ? full.substring(0, 4_000) + "...[truncated]" : full;
    }

    private JwtPrincipal resolvePrincipal(HttpServletRequest request) {
        // Prefer attributes stashed by ApiAuditPrincipalInterceptor (set while
        // SecurityContext was still alive inside DispatcherServlet).
        Object userId = request.getAttribute(ApiAuditPrincipalInterceptor.ATTR_USER_ID);
        Object tenantId = request.getAttribute(ApiAuditPrincipalInterceptor.ATTR_TENANT_ID);
        Object roles = request.getAttribute(ApiAuditPrincipalInterceptor.ATTR_ROLES);
        if (userId != null || tenantId != null || roles != null) {
            return new JwtPrincipal(
                    userId == null ? null : userId.toString(),
                    tenantId == null ? null : tenantId.toString(),
                    roles == null ? null : roles.toString());
        }

        // Fallback: SecurityContext (only useful when filter runs inside the security chain).
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return JwtPrincipal.EMPTY;
            String uid = null;
            String tid = null;
            if (auth.getPrincipal() instanceof Jwt jwt) {
                uid = jwt.getClaimAsString("sub");
                tid = jwt.getClaimAsString("tenant_id");
            }
            String r = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(s -> s.replace("ROLE_", ""))
                    .collect(Collectors.joining(","));
            return new JwtPrincipal(uid, tid, r.isEmpty() ? null : r);
        } catch (Exception e) {
            return JwtPrincipal.EMPTY;
        }
    }

    private record JwtPrincipal(String userId, String tenantId, String roles) {
        static final JwtPrincipal EMPTY = new JwtPrincipal(null, null, null);
    }
}
