package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.SourceOfIncomeOption;

import java.util.List;
import java.util.UUID;

public interface ManageSourceOfIncomeUseCase {

    SourceOfIncomeOption create(UUID tenantId, CreateSourceOfIncomeCommand command);
    SourceOfIncomeOption update(UUID tenantId, UUID id, UpdateSourceOfIncomeCommand command);
    SourceOfIncomeOption getById(UUID tenantId, UUID id);
    List<SourceOfIncomeOption> getAll(UUID tenantId);
    List<SourceOfIncomeOption> getActive(UUID tenantId);
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
