package com.ksa.financing.infra.security.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pre-authorization middleware that blocks requests issued by blacklisted
 * users, mobile numbers, NIDs, or devices.
 *
 * <p>Filter order: AFTER JWT auth (needs SecurityContext for JWT claims),
 * BEFORE Casbin authorization (block early so RBAC isn't even evaluated).</p>
 *
 * <p><b>Token-only model (default).</b> ALL identity attributes are pulled from
 * the JWT — never from headers or request bodies. The identity-service is
 * responsible for embedding the user's mobile, NID, and device id into the
 * access token at login time. This keeps the check uniform across every API,
 * regardless of payload, and prevents bypass by simply omitting headers.</p>
 *
 * <p>Extraction sources (token-only):
 * <ul>
 *   <li>userId   → JWT.sub</li>
 *   <li>mobile   → JWT claim (mobile_number / phone_number / mobile)</li>
 *   <li>NID      → JWT claim (national_id / nationalId / nid)</li>
 *   <li>deviceId → JWT claim (device_id / deviceId / device)</li>
 * </ul>
 *
 * <p>If a request carries no JWT (public / pre-auth endpoint), the filter passes
 * through — there is no identity to evaluate. Spring Security still enforces
 * authentication on protected endpoints upstream.</p>
 *
 * <p>On a hit the filter returns 403 with a localized error response and
 * audit-logs the violation.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class BlacklistGuardFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final BlacklistCacheService cacheService;
    private final BlacklistGuardProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        BlacklistEntry hit = evaluate(request);
        if (hit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Blacklist BLOCK: type={}, reason={}, path={}, method={}, ip={}",
                hit.type(), hit.reason(), path, request.getMethod(), clientIp(request));

        writeBlocked(request, response, hit);
    }

    /**
     * Walks the configured types and returns the first matching entry, if any.
     * <p>If no JWT is present (anonymous / pre-auth request), the filter passes
     * through — there's no identity to evaluate. Spring Security still enforces
     * authentication on protected endpoints upstream of this filter.</p>
     */
    private BlacklistEntry evaluate(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (authentication != null && authentication.getPrincipal() instanceof Jwt token) ? token : null;

        // Token-only mode + no JWT → nothing to check, pass through
        if (properties.isTokenOnly() && jwt == null) return null;

        for (BlacklistType type : properties.getCheckTypes()) {
            String rawValue = extract(type, request, jwt);
            if (rawValue == null || rawValue.isBlank()) continue;
            BlacklistEntry entry = cacheService.lookup(type, rawValue);
            if (entry != null) return entry;
        }
        return null;
    }

    /**
     * Extract identity attributes for a given blacklist type. ALL attributes
     * come from the JWT; headers / remote IP are only consulted when
     * {@code tokenOnly=false} (legacy behavior).
     */
    private String extract(BlacklistType type, HttpServletRequest request, Jwt jwt) {
        return switch (type) {
            case USER   -> jwt != null ? jwt.getSubject() : null;
            case MOBILE -> jwt != null ? BlacklistHasher.normalizeMobile(firstClaim(jwt, properties.getMobileClaims())) : null;
            case NID    -> jwt != null ? firstClaim(jwt, properties.getNidClaims()) : null;
            case DEVICE -> {
                String fromJwt = jwt != null ? firstClaim(jwt, properties.getDeviceClaims()) : null;
                if (fromJwt != null && !fromJwt.isBlank()) yield fromJwt;
                yield properties.isTokenOnly() ? null : firstHeader(request, properties.getDeviceIdHeaders());
            }
            case IP     -> null; // IP-based checks disabled in token-only model
        };
    }

    private static String firstClaim(Jwt jwt, java.util.List<String> claims) {
        for (String name : claims) {
            String value = jwt.getClaimAsString(name);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String firstHeader(HttpServletRequest request, java.util.List<String> headers) {
        for (String name : headers) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private boolean shouldSkip(String path) {
        for (String pattern : properties.getSkipPatterns()) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    private void writeBlocked(HttpServletRequest request,
                              HttpServletResponse response,
                              BlacklistEntry entry) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
        body.put("code", entry.type().errorCode());
        body.put("message", "Your account has been blocked.");
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
