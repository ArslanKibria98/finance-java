package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;

import java.util.List;
import java.util.UUID;

public interface ManagePurposeOfFinanceUseCase {

    PurposeOfFinanceEntry create(UUID tenantId, String code, String nameEn, String nameAr,
                                  String descriptionEn, String descriptionAr, int sortOrder, UUID createdBy);

    PurposeOfFinanceEntry update(UUID tenantId, UUID id, String nameEn, String nameAr,
                                  String descriptionEn, String descriptionAr, int sortOrder, boolean active);

    PurposeOfFinanceEntry getById(UUID tenantId, UUID id);

    List<PurposeOfFinanceEntry> listActive(UUID tenantId);

    List<PurposeOfFinanceEntry> listAll(UUID tenantId);

    void delete(UUID tenantId, UUID id);
}
