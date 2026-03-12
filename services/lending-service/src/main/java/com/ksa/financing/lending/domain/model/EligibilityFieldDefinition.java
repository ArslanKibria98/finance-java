package com.ksa.financing.lending.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for dynamic eligibility field definitions.
 * Admin-configurable fields like monthly_income, dependents, etc.
 * Zero framework imports.
 */
public class EligibilityFieldDefinition {

    private UUID id;
    private UUID tenantId;
    private String fieldKey;
    private String nameEn;
    private String nameAr;
    private String descriptionEn;
    private String descriptionAr;
    private String dataType;
    private String inputType;
    private String placeholderEn;
    private String placeholderAr;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private boolean required;
    private boolean active;
    private int sortOrder;

    private EligibilityFieldDefinition() {}

    public static EligibilityFieldDefinition create(UUID tenantId, String fieldKey, String nameEn, String nameAr,
                                                     String descriptionEn, String descriptionAr,
                                                     String dataType, String inputType,
                                                     String placeholderEn, String placeholderAr,
                                                     String unit, BigDecimal minValue, BigDecimal maxValue,
                                                     boolean required, int sortOrder) {
        if (fieldKey == null || fieldKey.isBlank()) {
            throw new IllegalArgumentException("Field key is required");
        }
        if (nameEn == null || nameEn.isBlank()) {
            throw new IllegalArgumentException("English name is required");
        }

        var field = new EligibilityFieldDefinition();
        field.tenantId = tenantId;
        field.fieldKey = fieldKey;
        field.nameEn = nameEn;
        field.nameAr = nameAr;
        field.descriptionEn = descriptionEn;
        field.descriptionAr = descriptionAr;
        field.dataType = dataType != null ? dataType : "NUMBER";
        field.inputType = inputType != null ? inputType : "TEXT";
        field.placeholderEn = placeholderEn;
        field.placeholderAr = placeholderAr;
        field.unit = unit;
        field.minValue = minValue;
        field.maxValue = maxValue;
        field.required = required;
        field.active = true;
        field.sortOrder = sortOrder;
        return field;
    }

    public static EligibilityFieldDefinition reconstitute(UUID id, UUID tenantId, String fieldKey,
                                                           String nameEn, String nameAr,
                                                           String descriptionEn, String descriptionAr,
                                                           String dataType, String inputType,
                                                           String placeholderEn, String placeholderAr,
                                                           String unit, BigDecimal minValue, BigDecimal maxValue,
                                                           boolean required, boolean active, int sortOrder) {
        var field = new EligibilityFieldDefinition();
        field.id = id;
        field.tenantId = tenantId;
        field.fieldKey = fieldKey;
        field.nameEn = nameEn;
        field.nameAr = nameAr;
        field.descriptionEn = descriptionEn;
        field.descriptionAr = descriptionAr;
        field.dataType = dataType;
        field.inputType = inputType;
        field.placeholderEn = placeholderEn;
        field.placeholderAr = placeholderAr;
        field.unit = unit;
        field.minValue = minValue;
        field.maxValue = maxValue;
        field.required = required;
        field.active = active;
        field.sortOrder = sortOrder;
        return field;
    }

    public void update(String nameEn, String nameAr, String descriptionEn, String descriptionAr,
                       String dataType, String inputType, String placeholderEn, String placeholderAr,
                       String unit, BigDecimal minValue, BigDecimal maxValue,
                       boolean required, boolean active, int sortOrder) {
        if (nameEn == null || nameEn.isBlank()) {
            throw new IllegalArgumentException("English name is required");
        }
        this.nameEn = nameEn;
        this.nameAr = nameAr;
        this.descriptionEn = descriptionEn;
        this.descriptionAr = descriptionAr;
        this.dataType = dataType;
        this.inputType = inputType;
        this.placeholderEn = placeholderEn;
        this.placeholderAr = placeholderAr;
        this.unit = unit;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.required = required;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public void deactivate() { this.active = false; }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFieldKey() { return fieldKey; }
    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public String getDescriptionEn() { return descriptionEn; }
    public String getDescriptionAr() { return descriptionAr; }
    public String getDataType() { return dataType; }
    public String getInputType() { return inputType; }
    public String getPlaceholderEn() { return placeholderEn; }
    public String getPlaceholderAr() { return placeholderAr; }
    public String getUnit() { return unit; }
    public BigDecimal getMinValue() { return minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public boolean isRequired() { return required; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
}
