package com.ksa.financing.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String USER_ID_CLAIM = "sub";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String tenantId = extractTenantId(request);
            String userId = extractUserId();

            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            }

            if (userId != null) {
                TenantContextHolder.setUserId(userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        // First try JWT claim
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String tenantId = jwt.getClaim(TENANT_ID_CLAIM);
            if (tenantId != null) {
                return tenantId;
            }
        }

        // Fallback to header
        return request.getHeader(TENANT_HEADER);
    }

    private String extractUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim(USER_ID_CLAIM);
        }
        return null;
    }
}
