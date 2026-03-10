package com.ksa.financing.infra.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Auto-configuration for Casbin authorization.
 * Activated when ksa.authorization.enabled=true in application.yml.
 *
 * <p>Creates the following beans:</p>
 * <ul>
 *   <li>{@link AuthorizationCacheService} — Redis cache operations</li>
 *   <li>{@link CasbinAuthorizationFilter} — servlet filter for every request</li>
 * </ul>
 *
 * <p>Services only need to:</p>
 * <ol>
 *   <li>Set ksa.authorization.enabled=true in application.yml</li>
 *   <li>Add .addFilterAfter(casbinFilter, BearerTokenAuthenticationFilter.class) in SecurityConfig</li>
 *   <li>Annotate controller methods with @SecuredEndpoint(obj=..., act=...)</li>
 * </ol>
 */
@Configuration
@ConditionalOnProperty(name = "ksa.authorization.enabled", havingValue = "true")
@EnableConfigurationProperties(AuthorizationProperties.class)
public class AuthorizationAutoConfiguration {

    @Bean
    public AuthorizationCacheService authorizationCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AuthorizationProperties properties) {
        return new AuthorizationCacheService(redisTemplate, objectMapper, properties);
    }

    @Bean("sdkCasbinAuthorizationFilter")
    @ConditionalOnMissingBean(name = "casbinAuthorizationFilter")
    public CasbinAuthorizationFilter casbinAuthorizationFilter(
            AuthorizationCacheService cacheService,
            AuthorizationProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        return new CasbinAuthorizationFilter(cacheService, properties, objectMapper, handlerMapping);
    }
}
