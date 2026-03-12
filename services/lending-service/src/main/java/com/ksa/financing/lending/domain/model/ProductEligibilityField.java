package com.ksa.financing.lending.domain.model;

import java.util.UUID;

/**
 * Links an eligibility field definition to a specific product.
 * Determines which eligibility questions are shown for a given product.
 * Zero framework imports.
 */
public class ProductEligibilityField {

    private UUID id;
    private UUID tenantId;
    private UUID productId;
    private UUID fieldId;
    private boolean required;
    private int sortOrder;

    // Enriched field (loaded via join)
    private EligibilityFieldDefinition fieldDefinition;

    private ProductEligibilityField() {}

    public static ProductEligibilityField create(UUID tenantId, UUID productId, UUID fieldId,
                                                  boolean required, int sortOrder) {
        if (productId == null) throw new IllegalArgumentException("Product ID is required");
        if (fieldId == null) throw new IllegalArgumentException("Field ID is required");

        var pef = new ProductEligibilityField();
        pef.tenantId = tenantId;
        pef.productId = productId;
        pef.fieldId = fieldId;
        pef.required = required;
        pef.sortOrder = sortOrder;
        return pef;
    }

    public static ProductEligibilityField reconstitute(UUID id, UUID tenantId, UUID productId,
                                                        UUID fieldId, boolean required, int sortOrder,
                                                        EligibilityFieldDefinition fieldDefinition) {
        var pef = new ProductEligibilityField();
        pef.id = id;
        pef.tenantId = tenantId;
        pef.productId = productId;
        pef.fieldId = fieldId;
        pef.required = required;
        pef.sortOrder = sortOrder;
        pef.fieldDefinition = fieldDefinition;
        return pef;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public UUID getFieldId() { return fieldId; }
    public boolean isRequired() { return required; }
    public int getSortOrder() { return sortOrder; }
    public EligibilityFieldDefinition getFieldDefinition() { return fieldDefinition; }
}
