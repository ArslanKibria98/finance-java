package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.FraudRule;
import com.ksa.financing.risk.domain.port.out.FraudRuleRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.FraudRuleJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FraudRuleRepositoryImpl implements FraudRuleRepository {

    private final JpaFraudRuleRepository jpaFraudRuleRepository;

    @Override
    public List<FraudRule> findAllByTenant(UUID tenantId) {
        return jpaFraudRuleRepository.findAllByTenantIdOrderByPriorityAsc(tenantId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<FraudRule> findByCategory(UUID tenantId, String category) {
        var cat = FraudRuleJpaEntity.FraudRuleCategory.valueOf(category.toUpperCase());
        return jpaFraudRuleRepository.findAllByTenantIdAndCategoryOrderByPriorityAsc(tenantId, cat)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FraudRule> findById(UUID id) {
        return jpaFraudRuleRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<FraudRule> findByRuleId(UUID tenantId, String ruleId) {
        return jpaFraudRuleRepository.findByTenantIdAndRuleId(tenantId, ruleId).map(this::toDomain);
    }

    @Override
    public FraudRule save(FraudRule rule) {
        var entity = toEntity(rule);
        var saved = jpaFraudRuleRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void delete(UUID id) {
        jpaFraudRuleRepository.deleteById(id);
    }

    private FraudRule toDomain(FraudRuleJpaEntity e) {
        var d = new FraudRule();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setRuleId(e.getRuleId());
        d.setScenarioName(e.getScenarioName());
        d.setScenarioNameAr(e.getScenarioNameAr());
        d.setCategory(e.getCategory() != null ? e.getCategory().name() : null);
        d.setDetectionLogic(e.getDetectionLogic());
        d.setDefaultAction(e.getDefaultAction() != null ? e.getDefaultAction().name() : null);
        d.setBlockType(e.getBlockType() != null ? e.getBlockType().name() : null);
        d.setStatus(e.getStatus() != null ? e.getStatus().name() : null);
        d.setParameters(e.getParameters());
        d.setPriority(e.getPriority());
        d.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null);
        d.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null);
        return d;
    }

    private FraudRuleJpaEntity toEntity(FraudRule d) {
        var e = new FraudRuleJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setRuleId(d.getRuleId());
        e.setScenarioName(d.getScenarioName());
        e.setScenarioNameAr(d.getScenarioNameAr());
        e.setCategory(d.getCategory() != null ? FraudRuleJpaEntity.FraudRuleCategory.valueOf(d.getCategory().toUpperCase()) : null);
        e.setDetectionLogic(d.getDetectionLogic());
        e.setDefaultAction(d.getDefaultAction() != null ? FraudRuleJpaEntity.FraudDecisionType.valueOf(d.getDefaultAction().toUpperCase()) : null);
        e.setBlockType(d.getBlockType() != null ? FraudRuleJpaEntity.FraudBlockType.valueOf(d.getBlockType().toUpperCase()) : null);
        e.setStatus(d.getStatus() != null ? FraudRuleJpaEntity.FraudRuleStatus.valueOf(d.getStatus().toUpperCase()) : null);
        e.setParameters(d.getParameters());
        e.setPriority(d.getPriority());
        e.setCreatedAt(d.getCreatedAt() != null ? d.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        e.setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        return e;
    }
}
