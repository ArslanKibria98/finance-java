package com.ksa.financing.identity.infrastructure.casbin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.identity.domain.port.out.PolicyEnforcerPort;
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
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CasbinAuthorizationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> SKIP_PATTERNS = List.of(
            "/api/v1/auth/**",
            "/api/v1/permissions/role/**",
            "/api/health/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    );

    private final PolicyEnforcerPort policyEnforcer;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        log.debug("Casbin filter: path={}, method={}, authorities={}", path, method, authorities);
        boolean allowed = false;

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority().replace("ROLE_", "");
            if (policyEnforcer.enforce(role, path, method)) {
                allowed = true;
                log.debug("Casbin ALLOWED: role={}, path={}, method={}", role, path, method);
                break;
            }
        }

        if (!allowed) {
            log.warn("Casbin DENIED: authorities={}, path={}, method={}", authorities, path, method);
            writeAccessDenied(response, path, method);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        for (String pattern : SKIP_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeAccessDenied(HttpServletResponse response, String path, String method) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("code", "IDENTITY.AUTHORIZATION.DENIED");
        body.put("message", "You are not authorized to access " + path + " with method " + method);
        body.put("path", path);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
