package com.ksa.financing.risk.infrastructure.persistence;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.port.out.ScenarioRuleRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.ScenarioRuleJpaEntity;
import com.ksa.financing.risk.infrastructure.persistence.mapper.RiskPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ScenarioRuleRepositoryImpl implements ScenarioRuleRepository {
    private final JpaScenarioRuleRepository jpa;

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "scenarioName", "scenarioNameAr"
    );

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
    public PageResponse<ScenarioRule> findAllByTenantId(UUID tenantId, PageQuery query) {
        Specification<ScenarioRuleJpaEntity> tenantSpec = (root, q, cb) ->
                cb.equal(root.get("tenantId"), tenantId);

        Specification<ScenarioRuleJpaEntity> dynamic = SpecificationBuilder.<ScenarioRuleJpaEntity>builder()
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<ScenarioRuleJpaEntity> page = jpa.findAll(
                tenantSpec.and(dynamic),
                query.toPageable());

        return PageResponse.from(page, RiskPersistenceMapper::toDomain);
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
