package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.SourceOfWealthOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface SourceOfWealthRepository {
    SourceOfWealthOption save(SourceOfWealthOption option);
    Optional<SourceOfWealthOption> findById(UUID tenantId, UUID id);
    PageResponse<SourceOfWealthOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<SourceOfWealthOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
    boolean existsByCode(UUID tenantId, String code);
    void softDelete(UUID tenantId, UUID id);
}
