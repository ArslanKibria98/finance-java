package com.ksa.financing.kycadapter.infrastructure.middleware;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Dedicated RestTemplate for outbound calls to middleware-third-party.
 * Lives in its own bean so it does not conflict with any other RestTemplate
 * in the application context.
 */
@Configuration
public class MiddlewareRestTemplateConfig {

    @Bean(name = "middlewareRestTemplate")
    public RestTemplate middlewareRestTemplate(
            @Value("${app.middleware.timeout-ms:30000}") int timeoutMs) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
