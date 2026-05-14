package com.ksa.financing.infra.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Auto-configuration for API audit. Active by default; can be disabled
 * per service with {@code ksa.audit.api.enabled=false}.
 *
 * <p>WebClient-related beans live in a nested {@link WebClientAuditConfig} so
 * the JVM never loads {@code ExchangeFilterFunction} on services that don't
 * have spring-webflux on the classpath.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "ksa.audit.api.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ApiAuditProperties.class)
public class ApiAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PiiMasker piiMasker(ObjectMapper objectMapper, ApiAuditProperties properties) {
        return new PiiMasker(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessContextExtractor businessContextExtractor(ObjectMapper objectMapper) {
        return new BusinessContextExtractor(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAuditLogger apiAuditLogger() {
        return new ApiAuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAuditFilter apiAuditFilter(ApiAuditProperties properties,
                                         ApiAuditLogger auditLogger,
                                         PiiMasker piiMasker,
                                         BusinessContextExtractor businessExtractor) {
        return new ApiAuditFilter(properties, auditLogger, piiMasker, businessExtractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAuditPrincipalInterceptor apiAuditPrincipalInterceptor() {
        return new ApiAuditPrincipalInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(name = "apiAuditWebMvcConfigurer")
    public WebMvcConfigurer apiAuditWebMvcConfigurer(ApiAuditPrincipalInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAuditRestTemplateInterceptor apiAuditRestTemplateInterceptor(
            ApiAuditProperties properties,
            ApiAuditLogger auditLogger,
            PiiMasker piiMasker,
            BusinessContextExtractor businessExtractor) {
        return new ApiAuditRestTemplateInterceptor(properties, auditLogger, piiMasker, businessExtractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAuditRestTemplateRegistrar apiAuditRestTemplateRegistrar(
            List<RestTemplate> restTemplates,
            ApiAuditRestTemplateInterceptor interceptor) {
        return new ApiAuditRestTemplateRegistrar(restTemplates, interceptor);
    }

    /**
     * Nested config so {@code ExchangeFilterFunction} is only referenced
     * when spring-webflux is on the classpath. Wrapping it in
     * {@code @ConditionalOnClass} on the inner class prevents the JVM
     * from resolving the method signature on stateless servlet apps.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    public static class WebClientAuditConfig {

        @Bean
        @ConditionalOnMissingBean
        public ApiAuditWebClientFilter apiAuditWebClientFilter(ApiAuditProperties properties,
                                                               ApiAuditLogger auditLogger,
                                                               PiiMasker piiMasker) {
            return new ApiAuditWebClientFilter(properties, auditLogger, piiMasker);
        }
    }
}
