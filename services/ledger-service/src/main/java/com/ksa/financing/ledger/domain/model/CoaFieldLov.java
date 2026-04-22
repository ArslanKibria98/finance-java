package com.ksa.financing.ledger.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class CoaFieldLov {

    private final UUID id;
    private final UUID tenantId;
    private final String fieldKey;
    private String fieldLabelEn;
    private String fieldLabelAr;
    private String category;
    private boolean mandatoryDefault;
    private int displayOrder;
    private CoaFieldStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    private CoaFieldLov(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.fieldKey = builder.fieldKey;
        this.fieldLabelEn = builder.fieldLabelEn;
        this.fieldLabelAr = builder.fieldLabelAr;
        this.category = builder.category;
        this.mandatoryDefault = builder.mandatoryDefault;
        this.displayOrder = builder.displayOrder;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.version = builder.version;
    }

    public static CoaFieldLov create(
            UUID tenantId,
            String fieldKey,
            String fieldLabelEn,
            String fieldLabelAr,
            String category,
            boolean mandatoryDefault,
            int displayOrder
    ) {
        if (tenantId == null) throw new IllegalArgumentException("TenantId cannot be null");
        if (fieldKey == null || fieldKey.isBlank()) throw new IllegalArgumentException("Field key cannot be blank");
        if (fieldLabelEn == null || fieldLabelEn.isBlank()) {
            throw new IllegalArgumentException("English field label cannot be blank");
        }
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Category cannot be blank");
        if (displayOrder < 0) throw new IllegalArgumentException("Display order cannot be negative");

        var now = LocalDateTime.now();
        return new Builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .fieldKey(fieldKey.trim().toUpperCase())
                .fieldLabelEn(fieldLabelEn.trim())
                .fieldLabelAr(fieldLabelAr)
                .category(category.trim().toUpperCase())
                .mandatoryDefault(mandatoryDefault)
                .displayOrder(displayOrder)
                .status(CoaFieldStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .version(1)
                .build();
    }

    public static CoaFieldLov reconstitute(Builder builder) {
        return new CoaFieldLov(builder);
    }

    public void update(String fieldLabelEn, String fieldLabelAr, String category, boolean mandatoryDefault, int displayOrder) {
        if (fieldLabelEn == null || fieldLabelEn.isBlank()) {
            throw new IllegalArgumentException("English field label cannot be blank");
        }
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Category cannot be blank");
        if (displayOrder < 0) throw new IllegalArgumentException("Display order cannot be negative");

        this.fieldLabelEn = fieldLabelEn.trim();
        this.fieldLabelAr = fieldLabelAr;
        this.category = category.trim().toUpperCase();
        this.mandatoryDefault = mandatoryDefault;
        this.displayOrder = displayOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = CoaFieldStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = CoaFieldStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFieldKey() { return fieldKey; }
    public String getFieldLabelEn() { return fieldLabelEn; }
    public String getFieldLabelAr() { return fieldLabelAr; }
    public String getCategory() { return category; }
    public boolean isMandatoryDefault() { return mandatoryDefault; }
    public int getDisplayOrder() { return displayOrder; }
    public CoaFieldStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID tenantId;
        private String fieldKey;
        private String fieldLabelEn;
        private String fieldLabelAr;
        private String category;
        private boolean mandatoryDefault;
        private int displayOrder;
        private CoaFieldStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private int version;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder fieldKey(String fieldKey) { this.fieldKey = fieldKey; return this; }
        public Builder fieldLabelEn(String fieldLabelEn) { this.fieldLabelEn = fieldLabelEn; return this; }
        public Builder fieldLabelAr(String fieldLabelAr) { this.fieldLabelAr = fieldLabelAr; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder mandatoryDefault(boolean mandatoryDefault) { this.mandatoryDefault = mandatoryDefault; return this; }
        public Builder displayOrder(int displayOrder) { this.displayOrder = displayOrder; return this; }
        public Builder status(CoaFieldStatus status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public CoaFieldLov build() { return new CoaFieldLov(this); }
    }
}
