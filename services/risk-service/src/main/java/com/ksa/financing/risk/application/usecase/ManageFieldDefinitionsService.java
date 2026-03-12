package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.port.in.ManageFieldDefinitionsUseCase;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageFieldDefinitionsService implements ManageFieldDefinitionsUseCase {

    private final CreditScoringRepository creditScoringRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringFieldDefinition> getAllFieldDefinitions(UUID tenantId) {
        log.info("Fetching all field definitions for tenant={}", tenantId);
        return creditScoringRepository.findAllFieldDefinitions(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditScoringFieldDefinition getFieldDefinitionById(UUID tenantId, UUID id) {
        log.info("Fetching field definition id={} for tenant={}", id, tenantId);
        return creditScoringRepository.findFieldDefinitionById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("FieldDefinition", id.toString()));
    }

    @Override
    @Transactional
    public CreditScoringFieldDefinition createFieldDefinition(UUID tenantId, CreateFieldDefinitionCommand command) {
        log.info("Creating field definition fieldKey={} for tenant={}", command.fieldKey(), tenantId);

        if (creditScoringRepository.fieldKeyExists(tenantId, command.fieldKey(), null)) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Field definition with key already exists: " + command.fieldKey());
        }

        var fieldDefinition = new CreditScoringFieldDefinition(
                null,
                tenantId,
                command.fieldKey(),
                command.nameEn(),
                command.nameAr(),
                command.dataType(),
                command.active(),
                command.sortOrder()
        );

        var saved = creditScoringRepository.saveFieldDefinition(fieldDefinition);
        log.info("Created field definition id={} fieldKey={}", saved.id(), saved.fieldKey());
        return saved;
    }

    @Override
    @Transactional
    public CreditScoringFieldDefinition updateFieldDefinition(UUID tenantId, UUID id, UpdateFieldDefinitionCommand command) {
        log.info("Updating field definition id={} for tenant={}", id, tenantId);

        var existing = creditScoringRepository.findFieldDefinitionById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("FieldDefinition", id.toString()));

        if (creditScoringRepository.fieldKeyExists(tenantId, command.fieldKey(), id)) {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Field definition with key already exists: " + command.fieldKey());
        }

        var updated = new CreditScoringFieldDefinition(
                existing.id(),
                existing.tenantId(),
                command.fieldKey(),
                command.nameEn(),
                command.nameAr(),
                command.dataType(),
                command.active(),
                command.sortOrder()
        );

        var result = creditScoringRepository.updateFieldDefinition(updated);
        log.info("Updated field definition id={} fieldKey={}", result.id(), result.fieldKey());
        return result;
    }

    @Override
    @Transactional
    public void deleteFieldDefinition(UUID tenantId, UUID id) {
        log.info("Deleting field definition id={} for tenant={}", id, tenantId);

        creditScoringRepository.findFieldDefinitionById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("FieldDefinition", id.toString()));

        creditScoringRepository.deleteFieldDefinition(tenantId, id);
        log.info("Deleted field definition id={}", id);
    }
}
