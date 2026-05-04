package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;

import java.util.UUID;

public interface GetCreditScoringFieldsUseCase {

    PageResponse<CreditScoringFieldDefinition> getActiveFieldDefinitions(UUID tenantId, PageQuery pageQuery);
}
