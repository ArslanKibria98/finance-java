package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;

import java.util.List;
import java.util.UUID;

public interface GetCreditScoringFieldsUseCase {

    List<CreditScoringFieldDefinition> getActiveFieldDefinitions(UUID tenantId);
}
