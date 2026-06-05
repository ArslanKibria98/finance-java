package com.ksa.financing.lending.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;

import java.util.Optional;
import java.util.UUID;

public interface PurposeOfFinanceRepository {

    PurposeOfFinanceEntry save(PurposeOfFinanceEntry entry);

    Optional<PurposeOfFinanceEntry> findById(UUID tenantId, UUID id);

    Optional<PurposeOfFinanceEntry> findByCode(UUID tenantId, String code);

    PageResponse<PurposeOfFinanceEntry> findAllActive(UUID tenantId, PageQuery query);

    PageResponse<PurposeOfFinanceEntry> findAll(UUID tenantId, PageQuery query);

    void delete(UUID tenantId, UUID id);
}
