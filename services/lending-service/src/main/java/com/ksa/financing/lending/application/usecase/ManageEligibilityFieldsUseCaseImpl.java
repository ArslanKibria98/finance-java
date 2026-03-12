package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.domain.model.EligibilityFieldDefinition;
import com.ksa.financing.lending.domain.model.ProductEligibilityField;
import com.ksa.financing.lending.domain.port.in.ManageEligibilityFieldsUseCase;
import com.ksa.financing.lending.domain.port.out.EligibilityFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageEligibilityFieldsUseCaseImpl implements ManageEligibilityFieldsUseCase {

    private static final UUID SYSTEM_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final EligibilityFieldRepository repository;

    // ══════════ Field Definitions ══════════

    @Override
    @Transactional
    public EligibilityFieldDefinition createDefinition(UUID tenantId, String fieldKey, String nameEn, String nameAr,
                                                        String descriptionEn, String descriptionAr,
                                                        String dataType, String inputType,
                                                        String placeholderEn, String placeholderAr,
                                                        String unit, BigDecimal minValue, BigDecimal maxValue,
                                                        boolean required, int sortOrder) {
        repository.findDefinitionByKey(tenantId, fieldKey).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.CONFLICT,
                    "Eligibility field with key already exists: " + fieldKey);
        });

        var definition = EligibilityFieldDefinition.create(tenantId, fieldKey, nameEn, nameAr,
                descriptionEn, descriptionAr, dataType, inputType,
                placeholderEn, placeholderAr, unit, minValue, maxValue, required, sortOrder);

        return repository.saveDefinition(definition);
    }

    @Override
    @Transactional
    public EligibilityFieldDefinition updateDefinition(UUID tenantId, UUID id, String nameEn, String nameAr,
                                                        String descriptionEn, String descriptionAr,
                                                        String dataType, String inputType,
                                                        String placeholderEn, String placeholderAr,
                                                        String unit, BigDecimal minValue, BigDecimal maxValue,
                                                        boolean required, boolean active, int sortOrder) {
        var definition = repository.findDefinitionById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("EligibilityFieldDefinition", id.toString()));

        definition.update(nameEn, nameAr, descriptionEn, descriptionAr,
                dataType, inputType, placeholderEn, placeholderAr,
                unit, minValue, maxValue, required, active, sortOrder);

        return repository.saveDefinition(definition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityFieldDefinition> listDefinitions(UUID tenantId) {
        return repository.findAllDefinitions(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public EligibilityFieldDefinition getDefinitionById(UUID tenantId, UUID id) {
        return repository.findDefinitionById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("EligibilityFieldDefinition", id.toString()));
    }

    @Override
    @Transactional
    public void deleteDefinition(UUID tenantId, UUID id) {
        repository.findDefinitionById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("EligibilityFieldDefinition", id.toString()));
        repository.deleteDefinition(tenantId, id);
    }

    // ══════════ Product-Field Mappings ══════════

    @Override
    @Transactional(readOnly = true)
    public List<ProductEligibilityField> getFieldsForProduct(UUID tenantId, UUID productId) {
        var fields = repository.findFieldsByProductId(tenantId, productId);

        // Fallback: if no product-specific fields, return system defaults
        if (fields.isEmpty()) {
            log.debug("No product-specific eligibility fields for product={}, falling back to system defaults", productId);
            var systemDefaults = repository.findActiveDefinitions(SYSTEM_TENANT);
            return systemDefaults.stream()
                    .map(def -> ProductEligibilityField.reconstitute(
                            null, tenantId, productId, def.getId(),
                            def.isRequired(), def.getSortOrder(), def))
                    .toList();
        }

        return fields;
    }

    @Override
    @Transactional
    public ProductEligibilityField assignFieldToProduct(UUID tenantId, UUID productId, UUID fieldId,
                                                         boolean required, int sortOrder) {
        repository.findDefinitionById(tenantId, fieldId)
                .or(() -> repository.findDefinitionById(SYSTEM_TENANT, fieldId))
                .orElseThrow(() -> NotFoundException.forEntity("EligibilityFieldDefinition", fieldId.toString()));

        var productField = ProductEligibilityField.create(tenantId, productId, fieldId, required, sortOrder);
        return repository.saveProductField(productField);
    }

    @Override
    @Transactional
    public void setProductFields(UUID tenantId, UUID productId, List<FieldAssignment> assignments) {
        repository.deleteAllProductFields(tenantId, productId);
        for (var assignment : assignments) {
            assignFieldToProduct(tenantId, productId, assignment.fieldId(),
                    assignment.required(), assignment.sortOrder());
        }
    }

    @Override
    @Transactional
    public void removeFieldFromProduct(UUID tenantId, UUID id) {
        repository.deleteProductField(tenantId, id);
    }
}
