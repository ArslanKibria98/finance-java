package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface PurposeOfFinanceRepository {
    PurposeOfFinanceOption save(PurposeOfFinanceOption option);
    Optional<PurposeOfFinanceOption> findById(UUID tenantId, UUID id);
    PageResponse<PurposeOfFinanceOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<PurposeOfFinanceOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
    boolean existsByCode(UUID tenantId, String code);
    void softDelete(UUID tenantId, UUID id);
}
