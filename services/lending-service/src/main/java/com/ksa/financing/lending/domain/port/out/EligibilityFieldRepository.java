package com.ksa.financing.lending.domain.port.out;

import com.ksa.financing.lending.domain.model.EligibilityFieldDefinition;
import com.ksa.financing.lending.domain.model.ProductEligibilityField;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for eligibility field persistence.
 */
public interface EligibilityFieldRepository {

    // Field definitions
    EligibilityFieldDefinition saveDefinition(EligibilityFieldDefinition definition);
    Optional<EligibilityFieldDefinition> findDefinitionById(UUID tenantId, UUID id);
    Optional<EligibilityFieldDefinition> findDefinitionByKey(UUID tenantId, String fieldKey);
    List<EligibilityFieldDefinition> findAllDefinitions(UUID tenantId);
    List<EligibilityFieldDefinition> findActiveDefinitions(UUID tenantId);
    void deleteDefinition(UUID tenantId, UUID id);

    // Product-field mappings
    ProductEligibilityField saveProductField(ProductEligibilityField productField);
    List<ProductEligibilityField> findFieldsByProductId(UUID tenantId, UUID productId);
    void deleteProductField(UUID tenantId, UUID id);
    void deleteAllProductFields(UUID tenantId, UUID productId);
}
