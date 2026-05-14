package com.ksa.financing.infra.security.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

/**
 * Read/write access to blacklist entries in Redis.
 *
 * <p>Key pattern: {@code {prefix}:{type}:{valueHash}} → JSON {@link BlacklistEntry}</p>
 *
 * <p>Reader is used by every service via {@link BlacklistGuardFilter}.
 * Writer is only used by the source-of-truth service (risk-service).</p>
 */
@RequiredArgsConstructor
@Slf4j
public class BlacklistCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BlacklistGuardProperties properties;

    /**
     * Look up a blacklist entry by type + raw value (caller normalises and hashes).
     *
     * @return the entry if hit, {@code null} if not found or Redis error.
     */
    public BlacklistEntry lookup(BlacklistType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;
        String hash = BlacklistHasher.sha256(rawValue);
        String key = type.redisKey(properties.getCacheKeyPrefix(), hash);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            BlacklistEntry entry = objectMapper.readValue(json, BlacklistEntry.class);
            if (entry.isExpired()) {
                redisTemplate.delete(key);
                return null;
            }
            return entry;
        } catch (Exception e) {
            log.warn("Blacklist Redis lookup failed for type={}, key={}: {}", type, key, e.getMessage());
            return null;
        }
    }

    public boolean isBlacklisted(BlacklistType type, String rawValue) {
        return lookup(type, rawValue) != null;
    }

    /**
     * Store an entry. Used by risk-service Redis sync.
     */
    public void put(BlacklistType type, String rawValue, String reason, Instant expiresAt) {
        if (rawValue == null || rawValue.isBlank()) return;
        String hash = BlacklistHasher.sha256(rawValue);
        String key = type.redisKey(properties.getCacheKeyPrefix(), hash);
        var entry = new BlacklistEntry(type, hash, reason, Instant.now(), expiresAt);
        try {
            String json = objectMapper.writeValueAsString(entry);
            if (expiresAt != null) {
                Duration ttl = Duration.between(Instant.now(), expiresAt);
                if (ttl.isPositive()) {
                    redisTemplate.opsForValue().set(key, json, ttl);
                    return;
                }
            }
            redisTemplate.opsForValue().set(key, json);
        } catch (Exception e) {
            log.error("Blacklist Redis put failed for type={}", type, e);
        }
    }

    public void remove(BlacklistType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return;
        String hash = BlacklistHasher.sha256(rawValue);
        String key = type.redisKey(properties.getCacheKeyPrefix(), hash);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Blacklist Redis remove failed for type={}", type, e);
        }
    }

    public void invalidateType(BlacklistType type) {
        try {
            String pattern = properties.getCacheKeyPrefix() + ":" + type.name().toLowerCase() + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} blacklist keys of type {}", keys.size(), type);
            }
        } catch (Exception e) {
            log.error("Blacklist Redis invalidate failed for type={}", type, e);
        }
    }
}
