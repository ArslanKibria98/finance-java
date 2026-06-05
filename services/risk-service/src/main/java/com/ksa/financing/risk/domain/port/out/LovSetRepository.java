package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.lov.LovSet;

import java.util.Optional;
import java.util.UUID;

public interface LovSetRepository {

    LovSet save(LovSet lovSet);

    Optional<LovSet> findById(UUID tenantId, UUID id);

    PageResponse<LovSet> findAllByTenantId(UUID tenantId, PageQuery pageQuery);

    PageResponse<LovSet> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);

    boolean existsByCode(UUID tenantId, String code);
}
