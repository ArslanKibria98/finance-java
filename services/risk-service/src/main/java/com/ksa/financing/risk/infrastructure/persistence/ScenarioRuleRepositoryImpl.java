package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.port.out.ScenarioRuleRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ScenarioRuleRepositoryImpl implements ScenarioRuleRepository {
    private final JpaScenarioRuleRepository jpa;

    @Override
    public ScenarioRule save(ScenarioRule rule) {
        return RiskPersistenceMapper.toDomain(jpa.save(RiskPersistenceMapper.toEntity(rule)));
    }
    @Override
    public Optional<ScenarioRule> findById(UUID tenantId, UUID id) {
        return jpa.findByIdAndTenantId(id, tenantId).map(RiskPersistenceMapper::toDomain);
    }
    @Override
    public List<ScenarioRule> findAllByTenantId(UUID tenantId) {
        return jpa.findAllByTenantId(tenantId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<ScenarioRule> findActiveByTenantId(UUID tenantId) {
        return jpa.findAllByTenantIdAndActiveTrue(tenantId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
    @Override
    public List<ScenarioRule> findActiveByTenantIdOrderByPriority(UUID tenantId) {
        return jpa.findAllByTenantIdAndActiveTrueOrderByPriorityAsc(tenantId).stream().map(RiskPersistenceMapper::toDomain).toList();
    }
}
