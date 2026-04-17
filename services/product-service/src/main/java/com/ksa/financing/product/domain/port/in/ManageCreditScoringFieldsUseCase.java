package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.CreditScoringFieldDefinition;

import java.util.List;
import java.util.UUID;

public interface ManageCreditScoringFieldsUseCase {

    List<CreditScoringFieldDefinition> listFieldDefinitions(UUID tenantId);
}
