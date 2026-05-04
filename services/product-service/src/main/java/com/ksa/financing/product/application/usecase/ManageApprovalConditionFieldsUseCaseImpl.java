package com.ksa.financing.product.application.usecase;

import com.ksa.financing.product.domain.model.ApprovalConditionFieldDefinition;
import com.ksa.financing.product.domain.port.in.ManageApprovalConditionFieldsUseCase;
import com.ksa.financing.product.domain.port.out.ApprovalConditionFieldRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageApprovalConditionFieldsUseCaseImpl implements ManageApprovalConditionFieldsUseCase {

    private final ApprovalConditionFieldRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApprovalConditionFieldDefinition> listFieldDefinitions(UUID tenantId, PageQuery pageQuery) {
        log.info("Listing approval condition field definitions for tenant: {}", tenantId);
        return repository.findAllWithOptions(tenantId, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalConditionFieldDefinition getFieldDefinition(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("ApprovalConditionField", id.toString()));
    }

    @Override
    @Transactional
    public ApprovalConditionFieldDefinition createFieldDefinition(UUID tenantId, CreateFieldCommand command) {
        if (repository.existsByFieldKey(tenantId, command.fieldKey())) {
            throw new BusinessException("APPROVAL.FIELD.DUPLICATE_KEY",
                    "Approval condition field with key already exists: " + command.fieldKey());
        }

        var field = new ApprovalConditionFieldDefinition(
                null, command.fieldKey(), command.nameEn(), command.nameAr(),
                command.dataType(), true, command.sortOrder(), Collections.emptyList());

        return repository.save(tenantId, field);
    }

    @Override
    @Transactional
    public ApprovalConditionFieldDefinition updateFieldDefinition(UUID tenantId, UUID id, UpdateFieldCommand command) {
        repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("ApprovalConditionField", id.toString()));

        var updated = new ApprovalConditionFieldDefinition(
                id, null, command.nameEn(), command.nameAr(),
                command.dataType(), command.active(), command.sortOrder(), Collections.emptyList());

        return repository.update(tenantId, id, updated);
    }

    @Override
    @Transactional
    public void deleteFieldDefinition(UUID tenantId, UUID id) {
        repository.delete(tenantId, id);
    }
}
