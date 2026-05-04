package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.SourceOfIncomeOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

public interface ManageSourceOfIncomeUseCase {

    SourceOfIncomeOption create(UUID tenantId, CreateSourceOfIncomeCommand command);
    SourceOfIncomeOption update(UUID tenantId, UUID id, UpdateSourceOfIncomeCommand command);
    SourceOfIncomeOption getById(UUID tenantId, UUID id);
    PageResponse<SourceOfIncomeOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<SourceOfIncomeOption> getActive(UUID tenantId, PageQuery pageQuery);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

    record CreateSourceOfIncomeCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        int displayOrder
    ) {}

    record UpdateSourceOfIncomeCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
