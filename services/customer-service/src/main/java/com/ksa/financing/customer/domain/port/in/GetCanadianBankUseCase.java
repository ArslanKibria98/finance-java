package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.CanadianBankOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

/**
 * Read-only access to Canadian financial institution reference data (LOV).
 */
public interface GetCanadianBankUseCase {

    CanadianBankOption getById(UUID tenantId, UUID id);
    PageResponse<CanadianBankOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<CanadianBankOption> getActive(UUID tenantId, PageQuery pageQuery);
}
