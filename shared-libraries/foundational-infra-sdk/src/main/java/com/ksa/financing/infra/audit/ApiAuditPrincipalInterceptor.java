package com.ksa.financing.infra.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.stream.Collectors;

/**
 * Snapshots the JWT principal (userId, tenantId, roles) onto a request
 * attribute while the SecurityContext is still alive (inside DispatcherServlet).
 * {@link ApiAuditFilter} runs outside the security chain and reads this
 * attribute when emitting the audit event.
 */
public class ApiAuditPrincipalInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "ksa.audit.userId";
    public static final String ATTR_TENANT_ID = "ksa.audit.tenantId";
    public static final String ATTR_ROLES = "ksa.audit.roles";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        snapshot(request);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
        if (request.getAttribute(ATTR_USER_ID) == null) {
            snapshot(request);
        }
    }

    private void snapshot(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;

        if (auth.getPrincipal() instanceof Jwt jwt) {
            request.setAttribute(ATTR_USER_ID, jwt.getClaimAsString("sub"));
            request.setAttribute(ATTR_TENANT_ID, jwt.getClaimAsString("tenant_id"));
        }
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(s -> s.replace("ROLE_", ""))
                .collect(Collectors.joining(","));
        if (!roles.isEmpty()) {
            request.setAttribute(ATTR_ROLES, roles);
        }
    }
}
