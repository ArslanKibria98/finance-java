package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

public interface ManagePurposeOfFinanceUseCase {

    PurposeOfFinanceOption create(UUID tenantId, CreatePurposeOfFinanceCommand command);
    PurposeOfFinanceOption update(UUID tenantId, UUID id, UpdatePurposeOfFinanceCommand command);
    PurposeOfFinanceOption getById(UUID tenantId, UUID id);
    PageResponse<PurposeOfFinanceOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<PurposeOfFinanceOption> getActive(UUID tenantId, PageQuery pageQuery);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

    record CreatePurposeOfFinanceCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        int displayOrder
    ) {}

    record UpdatePurposeOfFinanceCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
