package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.domain.model.EligibilityFieldDefinition;
import com.ksa.financing.lending.domain.model.ProductEligibilityField;
import com.ksa.financing.lending.domain.port.out.EligibilityFieldRepository;
import com.ksa.financing.lending.infrastructure.persistence.entity.EligibilityFieldDefinitionJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.entity.ProductEligibilityFieldJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EligibilityFieldRepositoryImpl implements EligibilityFieldRepository {

    private final JpaEligibilityFieldDefinitionRepository definitionRepo;
    private final JpaProductEligibilityFieldRepository productFieldRepo;

    // ══════════ Field Definitions ══════════

    @Override
    public EligibilityFieldDefinition saveDefinition(EligibilityFieldDefinition definition) {
        var entity = toDefinitionEntity(definition);
        var saved = definitionRepo.save(entity);
        return toDefinitionDomain(saved);
    }

    @Override
    public Optional<EligibilityFieldDefinition> findDefinitionById(UUID tenantId, UUID id) {
        return definitionRepo.findByTenantIdAndId(tenantId, id).map(this::toDefinitionDomain);
    }

    @Override
    public Optional<EligibilityFieldDefinition> findDefinitionByKey(UUID tenantId, String fieldKey) {
        return definitionRepo.findByTenantIdAndFieldKey(tenantId, fieldKey).map(this::toDefinitionDomain);
    }

    @Override
    public List<EligibilityFieldDefinition> findAllDefinitions(UUID tenantId) {
        return definitionRepo.findByTenantIdOrderBySortOrder(tenantId).stream()
                .map(this::toDefinitionDomain).toList();
    }

    @Override
    public List<EligibilityFieldDefinition> findActiveDefinitions(UUID tenantId) {
        return definitionRepo.findByTenantIdAndActiveOrderBySortOrder(tenantId, true).stream()
                .map(this::toDefinitionDomain).toList();
    }

    @Override
    public void deleteDefinition(UUID tenantId, UUID id) {
        definitionRepo.deleteByTenantIdAndId(tenantId, id);
    }

    // ══════════ Product-Field Mappings ══════════

    @Override
    public ProductEligibilityField saveProductField(ProductEligibilityField productField) {
        var entity = toProductFieldEntity(productField);
        var saved = productFieldRepo.save(entity);
        return toProductFieldDomain(saved);
    }

    @Override
    public List<ProductEligibilityField> findFieldsByProductId(UUID tenantId, UUID productId) {
        return productFieldRepo.findByTenantIdAndProductIdOrderBySortOrder(tenantId, productId).stream()
                .map(this::toProductFieldDomain).toList();
    }

    @Override
    public void deleteProductField(UUID tenantId, UUID id) {
        productFieldRepo.deleteByTenantIdAndId(tenantId, id);
    }

    @Override
    public void deleteAllProductFields(UUID tenantId, UUID productId) {
        productFieldRepo.deleteByTenantIdAndProductId(tenantId, productId);
    }

    // ══════════ Mappers ══════════

    private EligibilityFieldDefinitionJpaEntity toDefinitionEntity(EligibilityFieldDefinition d) {
        var e = new EligibilityFieldDefinitionJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setFieldKey(d.getFieldKey());
        e.setNameEn(d.getNameEn());
        e.setNameAr(d.getNameAr());
        e.setDescriptionEn(d.getDescriptionEn());
        e.setDescriptionAr(d.getDescriptionAr());
        e.setDataType(d.getDataType());
        e.setInputType(d.getInputType());
        e.setPlaceholderEn(d.getPlaceholderEn());
        e.setPlaceholderAr(d.getPlaceholderAr());
        e.setUnit(d.getUnit());
        e.setMinValue(d.getMinValue());
        e.setMaxValue(d.getMaxValue());
        e.setRequired(d.isRequired());
        e.setActive(d.isActive());
        e.setSortOrder(d.getSortOrder());
        return e;
    }

    private EligibilityFieldDefinition toDefinitionDomain(EligibilityFieldDefinitionJpaEntity e) {
        return EligibilityFieldDefinition.reconstitute(
                e.getId(), e.getTenantId(), e.getFieldKey(),
                e.getNameEn(), e.getNameAr(), e.getDescriptionEn(), e.getDescriptionAr(),
                e.getDataType(), e.getInputType(), e.getPlaceholderEn(), e.getPlaceholderAr(),
                e.getUnit(), e.getMinValue(), e.getMaxValue(),
                e.isRequired(), e.isActive(), e.getSortOrder()
        );
    }

    private ProductEligibilityFieldJpaEntity toProductFieldEntity(ProductEligibilityField pf) {
        var e = new ProductEligibilityFieldJpaEntity();
        e.setId(pf.getId());
        e.setTenantId(pf.getTenantId());
        e.setProductId(pf.getProductId());
        e.setFieldId(pf.getFieldId());
        e.setRequired(pf.isRequired());
        e.setSortOrder(pf.getSortOrder());
        return e;
    }

    private ProductEligibilityField toProductFieldDomain(ProductEligibilityFieldJpaEntity e) {
        EligibilityFieldDefinition def = null;
        if (e.getFieldDefinition() != null) {
            def = toDefinitionDomain(e.getFieldDefinition());
        }
        return ProductEligibilityField.reconstitute(
                e.getId(), e.getTenantId(), e.getProductId(),
                e.getFieldId(), e.isRequired(), e.getSortOrder(), def
        );
    }
}
