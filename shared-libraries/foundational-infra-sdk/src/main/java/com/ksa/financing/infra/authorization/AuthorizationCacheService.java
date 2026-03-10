package com.ksa.financing.infra.authorization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Redis cache layer for Casbin authorization policies.
 *
 * <p>Key pattern: "{prefix}:{role}" → JSON list of PolicyRecord</p>
 * <p>Example: "casbin:policies:admin" → [{"sub":"admin","obj":"customers","act":"create","effect":"ALLOW"}, ...]</p>
 *
 * <p>IDS writes policies to Redis on startup and on CRUD changes.
 * All other services only READ from Redis.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class AuthorizationCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthorizationProperties properties;

    private static final TypeReference<List<PolicyRecord>> POLICY_LIST_TYPE = new TypeReference<>() {};

    /**
     * Get all policies for a given role from Redis.
     *
     * @param role the Keycloak role (e.g., "admin", "csa", "customer")
     * @return list of policy records, or empty list if not cached
     */
    public List<PolicyRecord> getPoliciesForRole(String role) {
        String key = buildKey(role);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("No cached policies found for role: {}", role);
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, POLICY_LIST_TYPE);
        } catch (Exception e) {
            log.error("Failed to read policies from Redis for role: {}", role, e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if policies exist in Redis for a given role.
     */
    public boolean hasPoliciesForRole(String role) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(role)));
    }

    /**
     * Store policies for a role in Redis (called by IDS only).
     */
    public void storePoliciesForRole(String role, List<PolicyRecord> policies) {
        String key = buildKey(role);
        try {
            String json = objectMapper.writeValueAsString(policies);
            redisTemplate.opsForValue().set(key, json);
            log.info("Cached {} policies for role: {}", policies.size(), role);
        } catch (Exception e) {
            log.error("Failed to cache policies for role: {}", role, e);
        }
    }

    /**
     * Invalidate all cached policies (called by IDS on policy CRUD).
     */
    public void invalidateAll() {
        try {
            var keys = redisTemplate.keys(properties.getCacheKeyPrefix() + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cached policy keys", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to invalidate policy cache", e);
        }
    }

    private String buildKey(String role) {
        return properties.getCacheKeyPrefix() + ":" + role;
    }
}
