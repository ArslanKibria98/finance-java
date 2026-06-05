package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.CreditScoringFieldDefinition;

import java.util.List;
import java.util.UUID;

public interface CreditScoringFieldRepository {

    List<CreditScoringFieldDefinition> findAllWithOptions(UUID tenantId);
}
