package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.OccupationOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface OccupationRepository {
    OccupationOption save(OccupationOption option);
    Optional<OccupationOption> findById(UUID tenantId, UUID id);
    PageResponse<OccupationOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<OccupationOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
    boolean existsByCode(UUID tenantId, String code);
    void softDelete(UUID tenantId, UUID id);
}
