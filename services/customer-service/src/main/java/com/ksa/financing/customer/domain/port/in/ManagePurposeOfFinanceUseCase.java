package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.PurposeOfFinanceOption;

import java.util.List;
import java.util.UUID;

public interface ManagePurposeOfFinanceUseCase {

    PurposeOfFinanceOption create(UUID tenantId, CreatePurposeOfFinanceCommand command);
    PurposeOfFinanceOption update(UUID tenantId, UUID id, UpdatePurposeOfFinanceCommand command);
    PurposeOfFinanceOption getById(UUID tenantId, UUID id);
    List<PurposeOfFinanceOption> getAll(UUID tenantId);
    List<PurposeOfFinanceOption> getActive(UUID tenantId);
    void deactivate(UUID tenantId, UUID id);

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
