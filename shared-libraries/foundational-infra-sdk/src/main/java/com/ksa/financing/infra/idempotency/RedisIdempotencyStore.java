package com.ksa.financing.infra.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T executeIdempotent(String key, Supplier<T> operation) {
        return executeIdempotent(key, operation, DEFAULT_TTL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeIdempotent(String key, Supplier<T> operation, Duration ttl) {
        String fullKey = KEY_PREFIX + key;

        // Check if result exists
        Object cachedResult = redisTemplate.opsForValue().get(fullKey);
        if (cachedResult != null) {
            log.debug("Idempotent operation result found in cache for key: {}", key);
            return (T) cachedResult;
        }

        // Execute operation and store result
        T result = operation.get();
        store(key, result, ttl);
        log.debug("Idempotent operation executed and cached for key: {}", key);

        return result;
    }

    @Override
    public Optional<Object> get(String key) {
        String fullKey = KEY_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(fullKey);
        return Optional.ofNullable(value);
    }

    @Override
    public void store(String key, Object value) {
        store(key, value, DEFAULT_TTL);
    }

    @Override
    public void store(String key, Object value, Duration ttl) {
        String fullKey = KEY_PREFIX + key;
        redisTemplate.opsForValue().set(fullKey, value, ttl);
    }

    @Override
    public void delete(String key) {
        String fullKey = KEY_PREFIX + key;
        redisTemplate.delete(fullKey);
    }
}
