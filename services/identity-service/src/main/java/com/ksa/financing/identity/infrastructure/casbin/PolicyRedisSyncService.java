package com.ksa.financing.identity.infrastructure.casbin;

import com.ksa.financing.identity.domain.port.out.PolicyEnforcerPort;
import com.ksa.financing.infra.authorization.AuthorizationCacheService;
import com.ksa.financing.infra.authorization.PolicyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Syncs Casbin policies from the database to Redis on application startup
 * and whenever policies are modified via CRUD APIs.
 *
 * <p>This runs ONLY in identity-service (the source of truth for policies).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyRedisSyncService {

    private final PolicyEnforcerPort policyEnforcer;
    private final AuthorizationCacheService cacheService;

    /**
     * On IDS startup, load all policies from casbin_rule DB and push to Redis.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        syncPoliciesToRedis();
    }

    /**
     * Sync all policies from Casbin DB → Redis.
     * Called on startup and after any policy CRUD operation.
     */
    public void syncPoliciesToRedis() {
        try {
            policyEnforcer.reloadPolicy();
            List<List<String>> allPolicies = policyEnforcer.getAllPolicies();
            log.info("Syncing {} Casbin policies to Redis", allPolicies.size());

            // Invalidate all existing cached policies
            cacheService.invalidateAll();

            // Group policies by role (v0 = role)
            Map<String, List<PolicyRecord>> policiesByRole = allPolicies.stream()
                    .map(p -> new PolicyRecord(
                            p.get(0),   // sub (role)
                            p.get(1),   // obj (resource)
                            p.get(2),   // act (action)
                            "ALLOW"     // effect
                    ))
                    .collect(Collectors.groupingBy(PolicyRecord::sub));

            // Store each role's policies in Redis
            policiesByRole.forEach(cacheService::storePoliciesForRole);

            log.info("Successfully synced policies for {} roles to Redis: {}",
                    policiesByRole.size(), policiesByRole.keySet());
        } catch (Exception e) {
            log.error("Failed to sync policies to Redis", e);
        }
    }
}
