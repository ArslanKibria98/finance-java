package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.SourceOfWealthOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

public interface ManageSourceOfWealthUseCase {

    SourceOfWealthOption create(UUID tenantId, CreateSourceOfWealthCommand command);
    SourceOfWealthOption update(UUID tenantId, UUID id, UpdateSourceOfWealthCommand command);
    SourceOfWealthOption getById(UUID tenantId, UUID id);
    PageResponse<SourceOfWealthOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<SourceOfWealthOption> getActive(UUID tenantId, PageQuery pageQuery);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

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
