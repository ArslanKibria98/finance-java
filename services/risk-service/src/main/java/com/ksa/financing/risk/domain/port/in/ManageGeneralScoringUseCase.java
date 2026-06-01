package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.GeneralScoringConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * CRUD use case for GENERAL (onboarding) credit scoring criteria + config.
 * Sibling to {@link ManageCreditScoringUseCase} which handles per-PRODUCT criteria.
 */
public interface ManageGeneralScoringUseCase {

    List<CreditScoringCriteria> getCriteria(UUID tenantId);

    CreditScoringCriteria getCriteriaById(UUID tenantId, UUID id);

    /** Bulk replace-all (kept for backward compatibility / bulk import). */
    void saveCriteria(UUID tenantId, List<GeneralCriteriaCommand> commands);

    /** Add ONE criterion + its rules. Returns the persisted criterion with generated id. */
    CreditScoringCriteria createCriteria(UUID tenantId, GeneralCriteriaCommand command);

    /** Update ONE criterion + its rules (rules are replaced). */
    CreditScoringCriteria updateCriteria(UUID tenantId, UUID id, GeneralCriteriaCommand command);

    /** Delete ONE criterion (rules cascade). */
    void deleteCriteria(UUID tenantId, UUID id);

    void deleteAllCriteria(UUID tenantId);

    GeneralScoringConfig getConfig(UUID tenantId);

    GeneralScoringConfig updateConfig(UUID tenantId, UpdateGeneralConfigCommand command);

    record GeneralCriteriaCommand(
            UUID fieldDefinitionId,
            String customName,
            boolean custom,
            boolean enabled,
            int sortOrder,
            List<GeneralRuleCommand> rules
    ) {}

    record GeneralRuleCommand(
            String operator,
            String value,
            BigDecimal weight,
            BigDecimal percentage
    ) {}

    record UpdateGeneralConfigCommand(
            BigDecimal minPassPercentage,
            BigDecimal greenThreshold,
            BigDecimal amberThreshold,
            boolean enabled
    ) {}
}
