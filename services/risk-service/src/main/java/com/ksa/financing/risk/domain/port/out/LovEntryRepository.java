package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.lov.LovEntry;

import java.util.Optional;
import java.util.UUID;

public interface LovEntryRepository {

    LovEntry save(LovEntry entry);

    Optional<LovEntry> findById(UUID tenantId, UUID id);

    PageResponse<LovEntry> findByLovSetId(UUID tenantId, UUID lovSetId, PageQuery pageQuery);

    PageResponse<LovEntry> findActiveByLovSetId(UUID tenantId, UUID lovSetId, PageQuery pageQuery);

    Optional<LovEntry> findByFactorCode(UUID tenantId, UUID lovSetId, String factorCode);

    boolean existsByFactorCode(UUID tenantId, UUID lovSetId, String factorCode);
}
