package com.ksa.financing.infra.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Casbin authorization filter that runs on every authenticated request.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Skip public endpoints (health, auth, swagger, etc.)</li>
 *   <li>Resolve the controller method for this request</li>
 *   <li>Read {@link SecuredEndpoint} annotation → get obj + act</li>
 *   <li>Extract user's roles from SecurityContext (JWT)</li>
 *   <li>For each role, get policies from Redis via {@link AuthorizationCacheService}</li>
 *   <li>Match: does any policy ALLOW this role for obj + act?</li>
 *   <li>ALLOW → continue | DENY → 403 Forbidden</li>
 * </ol>
 */
@RequiredArgsConstructor
@Slf4j
public class CasbinAuthorizationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final AuthorizationCacheService cacheService;
    private final AuthorizationProperties properties;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Skip public endpoints
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Skip if not authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Resolve the handler method and read @SecuredEndpoint
        SecuredEndpoint secured = resolveSecuredEndpoint(request);
        if (secured == null) {
            // No @SecuredEndpoint annotation → allow through (endpoint is not secured by Casbin)
            filterChain.doFilter(request, response);
            return;
        }

        String obj = secured.obj();
        String act = secured.act();

        // 4. Extract roles from SecurityContext
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        log.debug("Casbin check: path={}, method={}, obj={}, act={}, authorities={}",
                path, method, obj, act, authorities);

        // 5. Check each role's policies from Redis
        boolean allowed = false;
        String matchedRole = null;

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority().replace("ROLE_", "");
            List<PolicyRecord> policies = cacheService.getPoliciesForRole(role);

            for (PolicyRecord policy : policies) {
                if (policy.matches(obj, act) && policy.isAllow()) {
                    allowed = true;
                    matchedRole = role;
                    break;
                }
            }
            if (allowed) break;
        }

        if (allowed) {
            log.debug("Casbin ALLOWED: role={}, obj={}, act={}, path={}", matchedRole, obj, act, path);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Casbin DENIED: authorities={}, obj={}, act={}, path={}", authorities, obj, act, path);
            writeAccessDenied(response, path, obj, act);
        }
    }

    /**
     * Resolve the @SecuredEndpoint annotation from the matched handler method.
     */
    private SecuredEndpoint resolveSecuredEndpoint(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                return handlerMethod.getMethodAnnotation(SecuredEndpoint.class);
            }
        } catch (Exception e) {
            log.debug("Could not resolve handler for request: {}", request.getRequestURI());
        }
        return null;
    }

    private boolean shouldSkip(String path) {
        for (String pattern : properties.getSkipPatterns()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeAccessDenied(HttpServletResponse response, String path,
                                    String obj, String act) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("code", "COMMON.AUTH.ACCESS_DENIED");
        body.put("message", "You do not have permission to perform '" + act + "' on '" + obj + "'");
        body.put("path", path);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
