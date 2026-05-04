package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;

import java.util.UUID;

public interface ManagePurposeOfFinanceUseCase {

    PurposeOfFinanceEntry create(UUID tenantId, String code, String nameEn, String nameAr,
                                  String descriptionEn, String descriptionAr, int sortOrder, UUID createdBy);

    PurposeOfFinanceEntry update(UUID tenantId, UUID id, String nameEn, String nameAr,
                                  String descriptionEn, String descriptionAr, int sortOrder, boolean active);

    PurposeOfFinanceEntry getById(UUID tenantId, UUID id);

    PageResponse<PurposeOfFinanceEntry> listActive(UUID tenantId, PageQuery query);

    PageResponse<PurposeOfFinanceEntry> listAll(UUID tenantId, PageQuery query);

    void delete(UUID tenantId, UUID id);
}
