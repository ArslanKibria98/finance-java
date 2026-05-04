package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.domain.model.ApprovalConditionFieldDefinition;

import java.util.UUID;

public interface ManageApprovalConditionFieldsUseCase {

    PageResponse<ApprovalConditionFieldDefinition> listFieldDefinitions(UUID tenantId, PageQuery pageQuery);

    ApprovalConditionFieldDefinition getFieldDefinition(UUID tenantId, UUID id);

    ApprovalConditionFieldDefinition createFieldDefinition(UUID tenantId, CreateFieldCommand command);

    ApprovalConditionFieldDefinition updateFieldDefinition(UUID tenantId, UUID id, UpdateFieldCommand command);

    void deleteFieldDefinition(UUID tenantId, UUID id);

    record CreateFieldCommand(
        String fieldKey,
        String nameEn,
        String nameAr,
        String dataType,
        int sortOrder
    ) {}

    record UpdateFieldCommand(
        String nameEn,
        String nameAr,
        String dataType,
        boolean active,
        int sortOrder
    ) {}
}
