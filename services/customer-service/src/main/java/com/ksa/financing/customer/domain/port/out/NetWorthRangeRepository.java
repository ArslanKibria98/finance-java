package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.NetWorthRangeOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface NetWorthRangeRepository {
    NetWorthRangeOption save(NetWorthRangeOption option);
    Optional<NetWorthRangeOption> findById(UUID tenantId, UUID id);
    PageResponse<NetWorthRangeOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<NetWorthRangeOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
    boolean existsByCode(UUID tenantId, String code);
    void softDelete(UUID tenantId, UUID id);
}
