package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.DelinquencyRuleAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaDelinquencyRuleAuditRepository extends JpaRepository<DelinquencyRuleAuditJpaEntity, UUID> {

    List<DelinquencyRuleAuditJpaEntity> findByTenantIdAndRuleIdOrderByChangedAtDesc(UUID tenantId, UUID ruleId);

    List<DelinquencyRuleAuditJpaEntity> findByTenantIdAndProductIdOrderByChangedAtDesc(UUID tenantId, UUID productId);
}
