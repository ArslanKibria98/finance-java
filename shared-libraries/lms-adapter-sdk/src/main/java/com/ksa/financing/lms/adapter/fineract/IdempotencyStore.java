package com.ksa.financing.lms.adapter.fineract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Store for managing idempotent operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * Checks if an idempotency key exists.
     */
    public boolean exists(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Stores a result with an idempotency key.
     */
    public void store(String idempotencyKey, Object result) {
        try {
            String key = KEY_PREFIX + idempotencyKey;
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
            log.debug("Stored idempotent result for key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result for idempotency", e);
            throw new RuntimeException("Failed to store idempotent result", e);
        }
    }

    /**
     * Retrieves a stored result.
     */
    public <T> T getResult(String idempotencyKey, Class<T> resultType) {
        try {
            String key = KEY_PREFIX + idempotencyKey;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, resultType);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize idempotent result", e);
            throw new RuntimeException("Failed to retrieve idempotent result", e);
        }
    }

    /**
     * Executes an operation idempotently.
     */
    public <T> T executeIdempotent(String idempotencyKey, IdempotentOperation<T> operation) {
        if (exists(idempotencyKey)) {
            log.debug("Returning cached result for idempotency key: {}", idempotencyKey);
            return getResult(idempotencyKey, operation.getResultType());
        }

        T result = operation.execute();
        store(idempotencyKey, result);
        return result;
    }

    /**
     * Functional interface for idempotent operations.
     */
    @FunctionalInterface
    public interface IdempotentOperation<T> {
        T execute();

        default Class<T> getResultType() {
            return (Class<T>) Object.class;
        }
    }
}