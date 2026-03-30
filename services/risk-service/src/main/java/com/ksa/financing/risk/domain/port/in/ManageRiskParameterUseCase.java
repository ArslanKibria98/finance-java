package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.parameter.FilledByType;
import com.ksa.financing.risk.domain.model.parameter.ParameterFlagType;
import com.ksa.financing.risk.domain.model.parameter.ParameterInputType;
import com.ksa.financing.risk.domain.model.parameter.RiskParameter;
import com.ksa.financing.risk.domain.model.parameter.RiskType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageRiskParameterUseCase {

    RiskParameter create(UUID tenantId, CreateRiskParameterCommand command);

    RiskParameter update(UUID tenantId, UUID parameterId, UpdateRiskParameterCommand command);

    RiskParameter getById(UUID tenantId, UUID parameterId);

    List<RiskParameter> getByRiskType(UUID tenantId, RiskType riskType);

    List<RiskParameter> getActiveByRiskType(UUID tenantId, RiskType riskType);

    List<RiskParameter> getByCategory(UUID tenantId, RiskType riskType, String category);

    void deactivate(UUID tenantId, UUID parameterId);

    record CreateRiskParameterCommand(
            RiskType riskType,
            String flow,
            String category,
            String subCategory,
            String questionEn,
            String questionAr,
            ParameterInputType inputType,
            UUID lovSetId,
            UUID parentParameterId,
            String parentTriggerValue,
            BigDecimal categoryWeight,
            String operator,
            String expectedValue,
            ParameterFlagType flagType,
            FilledByType filledBy,
            int displayOrder,
            String language
    ) {}

    record UpdateRiskParameterCommand(
            String questionEn,
            String questionAr,
            ParameterInputType inputType,
            UUID lovSetId,
            UUID parentParameterId,
            String parentTriggerValue,
            BigDecimal categoryWeight,
            String operator,
            String expectedValue,
            ParameterFlagType flagType,
            FilledByType filledBy,
            Integer displayOrder,
            String language
    ) {}
}
