package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.SourceOfFundsOption;

import java.util.List;
import java.util.UUID;

public interface ManageSourceOfFundsUseCase {

    SourceOfFundsOption create(UUID tenantId, CreateSourceOfFundsCommand command);
    SourceOfFundsOption update(UUID tenantId, UUID id, UpdateSourceOfFundsCommand command);
    SourceOfFundsOption getById(UUID tenantId, UUID id);
    List<SourceOfFundsOption> getAll(UUID tenantId);
    List<SourceOfFundsOption> getActive(UUID tenantId);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

    record CreateSourceOfFundsCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        int displayOrder
    ) {}

    record UpdateSourceOfFundsCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
