package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.port.out.DelinquencyRuleAuditRepository;
import com.ksa.financing.collections.infrastructure.persistence.entity.DelinquencyRuleAuditJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DelinquencyRuleAuditRepositoryImpl implements DelinquencyRuleAuditRepository {

    private final JpaDelinquencyRuleAuditRepository jpaRepository;

    @Override
    public void record(UUID tenantId, UUID ruleId, UUID productId, DelinquencyType type,
                       String action, String beforeJson, String afterJson,
                       UUID actorId, String actorRole, String correlationId) {
        var entity = new DelinquencyRuleAuditJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setRuleId(ruleId);
        entity.setProductId(productId);
        entity.setDelinquencyType((short) type.code());
        entity.setAction(action);
        entity.setBeforeSnapshot(beforeJson);
        entity.setAfterSnapshot(afterJson);
        entity.setActorId(actorId);
        entity.setActorRole(actorRole);
        entity.setCorrelationId(correlationId);
        entity.setChangedAt(LocalDateTime.now());
        jpaRepository.save(entity);
    }

    @Override
    public List<AuditEntry> findByRule(UUID tenantId, UUID ruleId) {
        return jpaRepository.findByTenantIdAndRuleIdOrderByChangedAtDesc(tenantId, ruleId).stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    public List<AuditEntry> findByProduct(UUID tenantId, UUID productId) {
        return jpaRepository.findByTenantIdAndProductIdOrderByChangedAtDesc(tenantId, productId).stream()
                .map(this::toEntry)
                .toList();
    }

    private AuditEntry toEntry(DelinquencyRuleAuditJpaEntity e) {
        return new AuditEntry(
                e.getId(), e.getTenantId(), e.getRuleId(), e.getProductId(),
                DelinquencyType.fromCode(e.getDelinquencyType()),
                e.getAction(), e.getBeforeSnapshot(), e.getAfterSnapshot(),
                e.getActorId(), e.getActorRole(), e.getCorrelationId(),
                e.getChangedAt());
    }
}
