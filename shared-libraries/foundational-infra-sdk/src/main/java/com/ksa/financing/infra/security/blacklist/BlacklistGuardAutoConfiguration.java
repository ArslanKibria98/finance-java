package com.ksa.financing.infra.security.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configuration for the blacklist middleware.
 * Active when {@code ksa.blacklist.enabled=true} and Redis is on the classpath.
 *
 * <p>Beans created:
 * <ul>
 *   <li>{@link BlacklistCacheService} — Redis read/write</li>
 *   <li>{@link BlacklistGuardFilter}  — servlet filter (NOT registered in chain by default;
 *       services wire it via SecurityFilterChain.addFilterAfter(...))</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "ksa.blacklist", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BlacklistGuardProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class BlacklistGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BlacklistCacheService blacklistCacheService(StringRedisTemplate redisTemplate,
                                                       ObjectMapper objectMapper,
                                                       BlacklistGuardProperties properties) {
        return new BlacklistCacheService(redisTemplate, objectMapper, properties);
    }

    @Bean("blacklistGuardFilter")
    @ConditionalOnMissingBean
    public BlacklistGuardFilter blacklistGuardFilter(BlacklistCacheService cacheService,
                                                     BlacklistGuardProperties properties,
                                                     ObjectMapper objectMapper) {
        return new BlacklistGuardFilter(cacheService, properties, objectMapper);
    }
}
