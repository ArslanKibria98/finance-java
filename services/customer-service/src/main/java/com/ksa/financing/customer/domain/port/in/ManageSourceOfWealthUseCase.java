package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.SourceOfWealthOption;

import java.util.List;
import java.util.UUID;

public interface ManageSourceOfWealthUseCase {

    SourceOfWealthOption create(UUID tenantId, CreateSourceOfWealthCommand command);
    SourceOfWealthOption update(UUID tenantId, UUID id, UpdateSourceOfWealthCommand command);
    SourceOfWealthOption getById(UUID tenantId, UUID id);
    List<SourceOfWealthOption> getAll(UUID tenantId);
    List<SourceOfWealthOption> getActive(UUID tenantId);
    void deactivate(UUID tenantId, UUID id);

    record CreateSourceOfWealthCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        int displayOrder
    ) {}

    record UpdateSourceOfWealthCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
