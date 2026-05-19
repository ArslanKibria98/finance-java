package com.ksa.financing.wallet.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Exposes a single {@link RestTemplate} bean so the SDK
 * {@code ApiAuditRestTemplateRegistrar} can attach the outbound audit
 * interceptor — every inter-service call from this service is then
 * shipped to Logstash with full request/response payloads (method, path,
 * headers, status, latency, request/response body).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }
}
