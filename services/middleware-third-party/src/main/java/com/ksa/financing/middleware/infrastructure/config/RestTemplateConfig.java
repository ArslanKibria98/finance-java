package com.ksa.financing.middleware.infrastructure.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Use Apache HttpClient 5 instead of the JDK {@code HttpURLConnection} so that:
 * <ul>
 *   <li>Non-2xx responses keep their body — we need it to surface Facia's
 *       {@code 400 / 422} validation errors back to the caller</li>
 *   <li>Per-request timeouts + connection pooling are configurable</li>
 * </ul>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10, TimeUnit.SECONDS)
                // Facia document-verification can take 60-180s on large images under
                // load. Give the outbound call 5 min so middleware never times out
                // before the caller (onboarding-workflow uses 5 min on its own client
                // and 6 min on the Temporal activity wrapper).
                .setResponseTimeout(5, TimeUnit.MINUTES)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}
