package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.FraudRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFraudRuleRepository extends JpaRepository<FraudRuleJpaEntity, UUID> {

    List<FraudRuleJpaEntity> findAllByTenantIdOrderByPriorityAsc(UUID tenantId);

    List<FraudRuleJpaEntity> findAllByTenantIdAndCategoryOrderByPriorityAsc(UUID tenantId, FraudRuleJpaEntity.FraudRuleCategory category);

    Optional<FraudRuleJpaEntity> findByTenantIdAndRuleId(UUID tenantId, String ruleId);
}
