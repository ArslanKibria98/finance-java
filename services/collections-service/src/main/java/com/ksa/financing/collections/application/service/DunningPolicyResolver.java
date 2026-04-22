package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningThresholds;
import com.ksa.financing.collections.domain.model.LateFeeConfig;
import com.ksa.financing.collections.domain.model.SimahReportingConfig;
import com.ksa.financing.collections.domain.port.out.DunningPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamically resolves the dunning policy that governs a given loan.
 *
 * Resolution order:
 *   1. Active product-specific policy (product_code = loan.productCode)
 *   2. Active tenant-wide default (is_default=true, product_code=NULL)
 *   3. Built-in safety fallback (DunningThresholds.defaults + disabled late fee + Simah defaults)
 *
 * The resolver is consulted by:
 *   - Payment processing (to compute late fees)
 *   - Repayment schedule aging (to apply grace period + DPD stage)
 *   - Delinquency engine (to drive Simah reporting, agent assignment, wallet freeze)
 *   - Scheduled dunning jobs (to pick up stage actions per policy)
 *
 * A short-lived in-memory cache avoids hammering the DB on high-frequency payment paths.
 * Cache is evicted on any policy mutation via {@link #evictTenantCache(UUID)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DunningPolicyResolver {

    private final DunningPolicyRepository policyRepository;

    private final Map<CacheKey, DunningPolicy> cache = new ConcurrentHashMap<>();

    /**
     * Primary entry point. Returns an effective policy view that is NEVER null —
     * guarantees callers always have a usable policy.
     */
    public ResolvedPolicy resolve(UUID tenantId, String productCode) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }

        // Step 1: try product-specific
        if (productCode != null && !productCode.isBlank()) {
            var productPolicy = cache.computeIfAbsent(
                    new CacheKey(tenantId, productCode),
                    k -> policyRepository
                            .findActiveByProductCode(k.tenantId(), k.productCode())
                            .orElse(null));
            if (productPolicy != null) {
                return ResolvedPolicy.fromPolicy(productPolicy, PolicySource.PRODUCT_SPECIFIC);
            }
        }

        // Step 2: tenant-wide default
        var tenantDefault = cache.computeIfAbsent(
                new CacheKey(tenantId, null),
                k -> policyRepository.findActiveDefault(k.tenantId()).orElse(null));
        if (tenantDefault != null) {
            return ResolvedPolicy.fromPolicy(tenantDefault, PolicySource.TENANT_DEFAULT);
        }

        // Step 3: hard-coded safety fallback
        log.warn("No dunning policy found for tenant={} product={}, using built-in defaults",
                tenantId, productCode);
        return ResolvedPolicy.builtInFallback();
    }

    public Optional<DunningPolicy> findRaw(UUID tenantId, String productCode) {
        var resolved = resolve(tenantId, productCode);
        return resolved.source() == PolicySource.BUILT_IN_FALLBACK
                ? Optional.empty()
                : Optional.ofNullable(resolved.policy());
    }

    /** Call after any create/update/delete/activate operation on policies for a tenant. */
    public void evictTenantCache(UUID tenantId) {
        cache.keySet().removeIf(k -> k.tenantId().equals(tenantId));
    }

    public void evictAll() {
        cache.clear();
    }

    private record CacheKey(UUID tenantId, String productCode) {}

    public enum PolicySource { PRODUCT_SPECIFIC, TENANT_DEFAULT, BUILT_IN_FALLBACK }

    /**
     * Effective policy view — unwraps aggregate access so callers don't need null checks
     * against the built-in fallback path.
     */
    public record ResolvedPolicy(
            DunningPolicy policy,
            DunningThresholds thresholds,
            LateFeeConfig lateFee,
            SimahReportingConfig simah,
            PolicySource source
    ) {
        static ResolvedPolicy fromPolicy(DunningPolicy p, PolicySource source) {
            return new ResolvedPolicy(p, p.getThresholds(), p.getLateFee(), p.getSimah(), source);
        }

        static ResolvedPolicy builtInFallback() {
            return new ResolvedPolicy(
                    null,
                    DunningThresholds.defaults(),
                    LateFeeConfig.disabled(),
                    SimahReportingConfig.defaults(),
                    PolicySource.BUILT_IN_FALLBACK);
        }

        public boolean hasPersistedPolicy() {
            return policy != null;
        }
    }
}
