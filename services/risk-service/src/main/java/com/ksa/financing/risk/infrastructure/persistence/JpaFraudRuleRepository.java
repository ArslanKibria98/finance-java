package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.FraudRuleJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFraudRuleRepository extends JpaRepository<FraudRuleJpaEntity, UUID>,
        JpaSpecificationExecutor<FraudRuleJpaEntity> {

    Page<FraudRuleJpaEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<FraudRuleJpaEntity> findAllByTenantIdAndCategory(UUID tenantId, FraudRuleJpaEntity.FraudRuleCategory category, Pageable pageable);

    Optional<FraudRuleJpaEntity> findByTenantIdAndRuleId(UUID tenantId, String ruleId);
}
