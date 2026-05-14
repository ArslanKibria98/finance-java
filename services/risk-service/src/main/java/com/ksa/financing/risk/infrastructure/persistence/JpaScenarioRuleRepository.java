package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.infrastructure.persistence.entity.ScenarioRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaScenarioRuleRepository extends JpaRepository<ScenarioRuleJpaEntity, UUID>, JpaSpecificationExecutor<ScenarioRuleJpaEntity> {
    Optional<ScenarioRuleJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ScenarioRuleJpaEntity> findAllByTenantId(UUID tenantId);
    List<ScenarioRuleJpaEntity> findAllByTenantIdAndActiveTrue(UUID tenantId);
    List<ScenarioRuleJpaEntity> findAllByTenantIdAndActiveTrueOrderByPriorityAsc(UUID tenantId);
}
