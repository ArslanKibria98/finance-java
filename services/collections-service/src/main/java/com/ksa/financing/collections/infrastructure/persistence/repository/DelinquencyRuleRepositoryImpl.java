package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyRuleId;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.port.out.DelinquencyRuleRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.DelinquencyRulePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class DelinquencyRuleRepositoryImpl implements DelinquencyRuleRepository {

    private final JpaDelinquencyRuleRepository jpaRuleRepo;
    private final JpaEarlySettlementConfigRepository jpaConfigRepo;
    private final DelinquencyRulePersistenceMapper mapper;

    @Override
    public DelinquencyRule save(DelinquencyRule rule) {
        var ruleEntity = mapper.toEntity(rule);
        var savedRuleEntity = jpaRuleRepo.save(ruleEntity);

        // Sync child early-settlement configs (rewrite-on-save: simple + correct)
        if (rule.getDelinquencyType() == DelinquencyType.EARLY_SETTLEMENT) {
            jpaConfigRepo.deleteAllByRuleId(savedRuleEntity.getId());
            jpaConfigRepo.flush();
            for (var cfg : rule.listEarlySettlementConfigs()) {
                jpaConfigRepo.save(mapper.toEntity(cfg));
            }
        }

        return hydrate(savedRuleEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DelinquencyRule> findById(UUID tenantId, DelinquencyRuleId id) {
        return jpaRuleRepo.findByTenantIdAndId(tenantId, id.getValue())
                .map(this::hydrate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DelinquencyRule> findActive(UUID tenantId, UUID productId, DelinquencyType type) {
        return jpaRuleRepo.findActive(tenantId, productId, (short) type.code())
                .map(this::hydrate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelinquencyRule> findAllForProduct(UUID tenantId, UUID productId) {
        return jpaRuleRepo.findAllByTenantIdAndProductId(tenantId, productId).stream()
                .map(this::hydrate)
                .toList();
    }

    @Override
    public void softDelete(UUID tenantId, DelinquencyRuleId id) {
        jpaRuleRepo.findByTenantIdAndId(tenantId, id.getValue()).ifPresent(e -> {
            e.setRecordState((short) 0);
            jpaRuleRepo.save(e);
        });
    }

    @Override
    public boolean removeEarlySettlementConfig(UUID tenantId, DelinquencyRuleId ruleId, UUID configId) {
        return jpaRuleRepo.findByTenantIdAndId(tenantId, ruleId.getValue())
                .map(rule -> jpaConfigRepo.deleteByRuleIdAndId(rule.getId(), configId) > 0)
                .orElse(false);
    }

    private DelinquencyRule hydrate(com.ksa.financing.collections.infrastructure.persistence.entity.DelinquencyRuleJpaEntity e) {
        var domain = mapper.toDomain(e);
        if (domain.getDelinquencyType() == DelinquencyType.EARLY_SETTLEMENT) {
            var configs = jpaConfigRepo.findAllByDelinquencyId(e.getId()).stream()
                    .map(mapper::toDomain)
                    .toList();
            domain.attachHydratedConfigs(configs);
        }
        return domain;
    }
}
