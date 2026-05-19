package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.OccupationOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

public interface ManageOccupationUseCase {

    OccupationOption create(UUID tenantId, CreateOccupationCommand command);
    OccupationOption update(UUID tenantId, UUID id, UpdateOccupationCommand command);
    OccupationOption getById(UUID tenantId, UUID id);
    PageResponse<OccupationOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<OccupationOption> getActive(UUID tenantId, PageQuery pageQuery);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

    record CreateOccupationCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        int displayOrder
    ) {}

    record UpdateOccupationCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
