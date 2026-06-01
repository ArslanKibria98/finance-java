package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.RelationshipOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.UUID;

public interface ManageRelationshipUseCase {

    RelationshipOption create(UUID tenantId, CreateRelationshipCommand command);
    RelationshipOption update(UUID tenantId, UUID id, UpdateRelationshipCommand command);
    RelationshipOption getById(UUID tenantId, UUID id);
    PageResponse<RelationshipOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<RelationshipOption> getActive(UUID tenantId, PageQuery pageQuery);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

    record CreateRelationshipCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        int displayOrder
    ) {}

    record UpdateRelationshipCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
