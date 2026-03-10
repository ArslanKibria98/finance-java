package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageCreditScoringUseCase {

    List<CreditScoringCriteria> getCriteriaByProduct(UUID tenantId, UUID productId);

    void saveCriteria(UUID tenantId, UUID productId, List<CreditScoringCriteriaCommand> criteria);

    void deleteCriteriaByProduct(UUID tenantId, UUID productId);

    record CreditScoringCriteriaCommand(
            UUID fieldDefinitionId,
            String customName,
            boolean custom,
            boolean enabled,
            int sortOrder,
            List<CreditScoringRuleCommand> rules
    ) {}

    record CreditScoringRuleCommand(
            String operator,
            String value,
            BigDecimal weight,
            BigDecimal percentage
    ) {}
}
