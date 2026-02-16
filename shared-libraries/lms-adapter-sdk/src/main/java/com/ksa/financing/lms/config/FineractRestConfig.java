package com.ksa.financing.lms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;

/**
 * REST client configuration for Fineract API communication.
 */
@Configuration
@RequiredArgsConstructor
public class FineractRestConfig {

    private final FineractConfig fineractConfig;

    @Bean
    public RestTemplate fineractRestTemplate() throws NoSuchAlgorithmException,
                                                      KeyStoreException,
                                                      KeyManagementException {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());

        // Configure Jackson for Fineract date formats
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(fineractObjectMapper());
        restTemplate.setMessageConverters(Collections.singletonList(converter));

        // Add interceptor for authentication and headers
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();

            // Basic authentication
            String auth = fineractConfig.getUsername() + ":" + fineractConfig.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

            // Fineract tenant header
            headers.add("Fineract-Platform-TenantId", fineractConfig.getTenantId());

            // Content type
            headers.setContentType(MediaType.APPLICATION_JSON);

            return execution.execute(request, body);
        });

        return restTemplate;
    }

    @Bean
    public ObjectMapper fineractObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Fineract specific date format
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        return objectMapper;
    }

    private HttpComponentsClientHttpRequestFactory clientHttpRequestFactory()
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory();

        factory.setHttpClient(httpClient());
        factory.setConnectTimeout(fineractConfig.getConnectTimeoutMs());
        factory.setConnectionRequestTimeout(fineractConfig.getConnectTimeoutMs());

        return factory;
    }

    private HttpClient httpClient() throws NoSuchAlgorithmException,
                                          KeyStoreException,
                                          KeyManagementException {

        // Connection pool configuration
        PoolingHttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(fineractConfig.getMaxConnections());
        connectionManager.setDefaultMaxPerRoute(fineractConfig.getMaxConnectionsPerRoute());

        HttpClientBuilder builder = HttpClientBuilder.create()
            .setConnectionManager(connectionManager);

        // Disable SSL validation for development (not recommended for production)
        if (!fineractConfig.isSslValidation()) {
            SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

            SSLConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext, (hostname, session) -> true);

            builder.setConnectionManager(connectionManager);
        }

        return builder.build();
    }
}