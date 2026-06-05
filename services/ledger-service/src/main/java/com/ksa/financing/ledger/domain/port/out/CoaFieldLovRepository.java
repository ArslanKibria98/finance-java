package com.ksa.financing.ledger.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.domain.model.CoaFieldLov;

import java.util.Optional;
import java.util.UUID;

public interface CoaFieldLovRepository {

    CoaFieldLov save(CoaFieldLov fieldLov);

    Optional<CoaFieldLov> findById(UUID tenantId, UUID id);

    Optional<CoaFieldLov> findByFieldKey(UUID tenantId, String fieldKey);

    PageResponse<CoaFieldLov> findAllByTenant(UUID tenantId, PageQuery pageQuery);

    PageResponse<CoaFieldLov> findAllActiveByTenant(UUID tenantId, PageQuery pageQuery);

    boolean existsByFieldKey(UUID tenantId, String fieldKey);
}
