package com.ksa.financing.lending.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lms.adapter.fineract.FineractClient;
import com.ksa.financing.lms.adapter.fineract.FineractLmsAdapter;
import com.ksa.financing.lms.adapter.fineract.FineractMapper;
import com.ksa.financing.lms.adapter.fineract.IdempotencyStore;
import com.ksa.financing.lms.config.FineractConfig;
import com.ksa.financing.lms.port.LmsPort;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LmsConfig {

    @Bean
    public LmsPort lmsPort(
            FineractClient fineractClient,
            FineractMapper fineractMapper,
            RedissonClient redissonClient,
            IdempotencyStore idempotencyStore) {
        return new FineractLmsAdapter(fineractClient, fineractMapper, redissonClient, idempotencyStore);
    }

    @Bean
    public FineractClient fineractClient(
            RestTemplate restTemplate,
            FineractConfig fineractConfig) {
        return new FineractClient(restTemplate, fineractConfig);
    }

    @Bean
    public FineractConfig fineractConfig(
            @Value("${lms.fineract.base-url:${FINERACT_BASE_URL:https://localhost:8443/fineract-provider/api/v1}}") String baseUrl,
            @Value("${lms.fineract.username:${FINERACT_USERNAME:mifos}}") String username,
            @Value("${lms.fineract.password:${FINERACT_PASSWORD:password}}") String password,
            @Value("${lms.fineract.tenant-id:default}") String tenantId) {
        var config = new FineractConfig();
        config.setBaseUrl(baseUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setTenantId(tenantId);
        return config;
    }

    @Bean
    public FineractMapper fineractMapper() {
        return new FineractMapper();
    }

    @Bean
    public IdempotencyStore idempotencyStore(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        return new IdempotencyStore(redisTemplate, objectMapper);
    }
}
