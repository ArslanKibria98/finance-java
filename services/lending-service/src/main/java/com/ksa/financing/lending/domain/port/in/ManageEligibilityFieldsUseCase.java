package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.lending.domain.model.EligibilityFieldDefinition;
import com.ksa.financing.lending.domain.model.ProductEligibilityField;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input port for managing eligibility field definitions and product-field mappings.
 */
public interface ManageEligibilityFieldsUseCase {

    // ══════════ Field Definitions ══════════

    EligibilityFieldDefinition createDefinition(UUID tenantId, String fieldKey, String nameEn, String nameAr,
                                                 String descriptionEn, String descriptionAr,
                                                 String dataType, String inputType,
                                                 String placeholderEn, String placeholderAr,
                                                 String unit, BigDecimal minValue, BigDecimal maxValue,
                                                 boolean required, int sortOrder);

    EligibilityFieldDefinition updateDefinition(UUID tenantId, UUID id, String nameEn, String nameAr,
                                                 String descriptionEn, String descriptionAr,
                                                 String dataType, String inputType,
                                                 String placeholderEn, String placeholderAr,
                                                 String unit, BigDecimal minValue, BigDecimal maxValue,
                                                 boolean required, boolean active, int sortOrder);

    List<EligibilityFieldDefinition> listDefinitions(UUID tenantId);

    EligibilityFieldDefinition getDefinitionById(UUID tenantId, UUID id);

    void deleteDefinition(UUID tenantId, UUID id);

    // ══════════ Product-Field Mappings ══════════

    /**
     * Get eligibility fields configured for a specific product.
     * Falls back to system defaults if no product-specific fields exist.
     */
    List<ProductEligibilityField> getFieldsForProduct(UUID tenantId, UUID productId);

    /**
     * Assign a field to a product.
     */
    ProductEligibilityField assignFieldToProduct(UUID tenantId, UUID productId, UUID fieldId,
                                                  boolean required, int sortOrder);

    /**
     * Replace all field assignments for a product.
     */
    void setProductFields(UUID tenantId, UUID productId, List<FieldAssignment> assignments);

    /**
     * Remove a field assignment from a product.
     */
    void removeFieldFromProduct(UUID tenantId, UUID id);

    record FieldAssignment(UUID fieldId, boolean required, int sortOrder) {}
}
