package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;

import java.util.List;
import java.util.UUID;

public interface ManageFieldDefinitionsUseCase {

    List<CreditScoringFieldDefinition> getAllFieldDefinitions(UUID tenantId);

    CreditScoringFieldDefinition getFieldDefinitionById(UUID tenantId, UUID id);

    CreditScoringFieldDefinition createFieldDefinition(UUID tenantId, CreateFieldDefinitionCommand command);

    CreditScoringFieldDefinition updateFieldDefinition(UUID tenantId, UUID id, UpdateFieldDefinitionCommand command);

    void deleteFieldDefinition(UUID tenantId, UUID id);

    record CreateFieldDefinitionCommand(
            String fieldKey,
            String nameEn,
            String nameAr,
            String dataType,
            boolean active,
            int sortOrder
    ) {}

    record UpdateFieldDefinitionCommand(
            String fieldKey,
            String nameEn,
            String nameAr,
            String dataType,
            boolean active,
            int sortOrder
    ) {}
}
