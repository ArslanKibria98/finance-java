package com.ksa.financing.infra.authorization;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ksa.authorization")
public class AuthorizationProperties {

    /**
     * Enable/disable Casbin authorization filter.
     * When disabled, all requests pass through without policy check.
     */
    private boolean enabled = false;

    /**
     * Redis key prefix for cached policies.
     */
    private String cacheKeyPrefix = "casbin:policies";

    /**
     * URL patterns to skip authorization (public endpoints).
     * Supports Ant-style patterns.
     */
    private List<String> skipPatterns = new ArrayList<>(List.of(
            "/api/v1/auth/**",
            "/api/health/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/internal/**",
            "/error"
    ));
}
