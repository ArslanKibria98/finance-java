package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.SourceOfFundsOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface SourceOfFundsRepository {
    SourceOfFundsOption save(SourceOfFundsOption option);
    Optional<SourceOfFundsOption> findById(UUID tenantId, UUID id);
    PageResponse<SourceOfFundsOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<SourceOfFundsOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
    boolean existsByCode(UUID tenantId, String code);
    void softDelete(UUID tenantId, UUID id);
}
