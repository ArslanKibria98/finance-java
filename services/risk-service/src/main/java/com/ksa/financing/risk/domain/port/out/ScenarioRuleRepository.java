package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScenarioRuleRepository {

    ScenarioRule save(ScenarioRule rule);

    Optional<ScenarioRule> findById(UUID tenantId, UUID id);

    List<ScenarioRule> findAllByTenantId(UUID tenantId);

    PageResponse<ScenarioRule> findAllByTenantId(UUID tenantId, PageQuery query);

    List<ScenarioRule> findActiveByTenantId(UUID tenantId);

    List<ScenarioRule> findActiveByTenantIdOrderByPriority(UUID tenantId);
}
