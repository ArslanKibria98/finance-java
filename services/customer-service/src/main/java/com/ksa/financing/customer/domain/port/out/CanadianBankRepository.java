package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.CanadianBankOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface CanadianBankRepository {
    Optional<CanadianBankOption> findById(UUID tenantId, UUID id);
    PageResponse<CanadianBankOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<CanadianBankOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
}
