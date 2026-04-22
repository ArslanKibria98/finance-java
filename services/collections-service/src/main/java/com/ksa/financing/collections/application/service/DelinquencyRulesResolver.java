package com.ksa.financing.collections.application.service;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.port.out.DelinquencyRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the six {@link DelinquencyRule} rows for a given (tenant, product) tuple.
 * Consumed by:
 *   - {@code DelinquencyEngine}     → LATE_PAYMENT penalty accrual on payment tick
 *   - {@code ManageSettlementUseCase} → EARLY_SETTLEMENT discount during Ibra quote
 *   - {@code RepaymentScheduleController} → eligibility flag on installments API
 *
 * Cached in-memory per (tenant, product). Evict via {@link #evictTenant(UUID)} after
 * any admin upsert/delete on the delinquency rule set.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DelinquencyRulesResolver {

    private final DelinquencyRuleRepository ruleRepository;

    private final Map<CacheKey, Map<DelinquencyType, DelinquencyRule>> cache = new ConcurrentHashMap<>();

    /**
     * Returns all active rules for the (tenant, product) tuple, keyed by type.
     * Missing types simply aren't present in the map — callers use {@link #rule(Map, DelinquencyType)}
     * to extract safely.
     */
    public Map<DelinquencyType, DelinquencyRule> rulesFor(UUID tenantId, UUID productId) {
        if (tenantId == null || productId == null) {
            return Map.of();
        }
        return cache.computeIfAbsent(new CacheKey(tenantId, productId), this::load);
    }

    public Optional<DelinquencyRule> rule(UUID tenantId, UUID productId, DelinquencyType type) {
        return Optional.ofNullable(rulesFor(tenantId, productId).get(type));
    }

    public static Optional<DelinquencyRule> rule(Map<DelinquencyType, DelinquencyRule> rules, DelinquencyType type) {
        return Optional.ofNullable(rules.get(type));
    }

    public void evictTenant(UUID tenantId) {
        cache.keySet().removeIf(k -> k.tenantId().equals(tenantId));
    }

    public void evictAll() {
        cache.clear();
    }

    private Map<DelinquencyType, DelinquencyRule> load(CacheKey key) {
        var rules = ruleRepository.findAllForProduct(key.tenantId(), key.productId());
        var byType = new EnumMap<DelinquencyType, DelinquencyRule>(DelinquencyType.class);
        for (var r : rules) {
            if (r.getRecordState() == 1) {
                byType.put(r.getDelinquencyType(), r);
            }
        }
        log.debug("Loaded {} delinquency rule(s) for tenant={} product={}", byType.size(), key.tenantId(), key.productId());
        return Map.copyOf(byType);
    }

    private record CacheKey(UUID tenantId, UUID productId) {}
}
