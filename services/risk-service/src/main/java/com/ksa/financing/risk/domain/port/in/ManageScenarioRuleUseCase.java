package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.model.scenario.ThirdPartyCheckType;
import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;

import java.util.List;
import java.util.UUID;

public interface ManageScenarioRuleUseCase {

    ScenarioRule create(UUID tenantId, CreateScenarioRuleCommand command);

    ScenarioRule update(UUID tenantId, UUID ruleId, UpdateScenarioRuleCommand command);

    ScenarioRule getById(UUID tenantId, UUID ruleId);

    List<ScenarioRule> getAll(UUID tenantId);

    List<ScenarioRule> getActive(UUID tenantId);

    void deactivate(UUID tenantId, UUID ruleId);

    record CreateScenarioRuleCommand(
            String scenarioName,
            String scenarioNameAr,
            String triggerRiskStatus,
            Boolean triggerPepFlag,
            ThirdPartyCheckType triggerThirdPartyCheckType,
            String triggerThirdPartyResult,
            AccountStatus resultingAccountStatus,
            ComplianceStatus resultingComplianceStatus,
            boolean requiresManualReview,
            String notifyRole,
            int priority,
            int slaDurationHours
    ) {}

    record UpdateScenarioRuleCommand(
            String scenarioName,
            String scenarioNameAr,
            String triggerRiskStatus,
            Boolean triggerPepFlag,
            ThirdPartyCheckType triggerThirdPartyCheckType,
            String triggerThirdPartyResult,
            AccountStatus resultingAccountStatus,
            ComplianceStatus resultingComplianceStatus,
            Boolean requiresManualReview,
            String notifyRole,
            Integer priority,
            Integer slaDurationHours
    ) {}
}
