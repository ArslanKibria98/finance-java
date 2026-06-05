package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.parameter.RiskParameter;
import com.ksa.financing.risk.domain.model.parameter.RiskType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskParameterRepository {

    RiskParameter save(RiskParameter parameter);

    Optional<RiskParameter> findById(UUID tenantId, UUID id);

    List<RiskParameter> findByRiskType(UUID tenantId, RiskType riskType);

    List<RiskParameter> findActiveByRiskType(UUID tenantId, RiskType riskType);

    List<RiskParameter> findByCategory(UUID tenantId, RiskType riskType, String category);

    List<RiskParameter> findByParentId(UUID tenantId, UUID parentParameterId);
}
