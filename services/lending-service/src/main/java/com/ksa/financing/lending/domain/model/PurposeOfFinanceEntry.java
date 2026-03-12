package com.ksa.financing.lending.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for purpose of finance reference data.
 * Configurable per tenant — replaces the hardcoded PurposeOfFinance enum.
 */
public class PurposeOfFinanceEntry {

    private final UUID id;
    private final UUID tenantId;
    private String code;
    private String nameEn;
    private String nameAr;
    private String descriptionEn;
    private String descriptionAr;
    private boolean active;
    private int sortOrder;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final UUID createdBy;
    private int version;

    private PurposeOfFinanceEntry(UUID id, UUID tenantId, String code, String nameEn,
                                   UUID createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.nameEn = nameEn;
        this.active = true;
        this.sortOrder = 0;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.version = 1;
    }

    public static PurposeOfFinanceEntry create(UUID tenantId, String code, String nameEn,
                                                String nameAr, String descriptionEn, String descriptionAr,
                                                int sortOrder, UUID createdBy) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID cannot be null");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Code cannot be empty");
        if (nameEn == null || nameEn.isBlank()) throw new IllegalArgumentException("English name cannot be empty");

        var entry = new PurposeOfFinanceEntry(UUID.randomUUID(), tenantId, code.toUpperCase(), nameEn,
                createdBy, LocalDateTime.now());
        entry.nameAr = nameAr;
        entry.descriptionEn = descriptionEn;
        entry.descriptionAr = descriptionAr;
        entry.sortOrder = sortOrder;
        return entry;
    }

    public static PurposeOfFinanceEntry reconstitute(UUID id, UUID tenantId, String code, String nameEn,
                                                      String nameAr, String descriptionEn, String descriptionAr,
                                                      boolean active, int sortOrder, LocalDateTime createdAt,
                                                      LocalDateTime updatedAt, UUID createdBy, int version) {
        var entry = new PurposeOfFinanceEntry(id, tenantId, code, nameEn, createdBy, createdAt);
        entry.nameAr = nameAr;
        entry.descriptionEn = descriptionEn;
        entry.descriptionAr = descriptionAr;
        entry.active = active;
        entry.sortOrder = sortOrder;
        entry.updatedAt = updatedAt;
        entry.version = version;
        return entry;
    }

    public void update(String nameEn, String nameAr, String descriptionEn,
                       String descriptionAr, int sortOrder, boolean active) {
        if (nameEn == null || nameEn.isBlank()) throw new IllegalArgumentException("English name cannot be empty");
        this.nameEn = nameEn;
        this.nameAr = nameAr;
        this.descriptionEn = descriptionEn;
        this.descriptionAr = descriptionAr;
        this.sortOrder = sortOrder;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public String getDescriptionEn() { return descriptionEn; }
    public String getDescriptionAr() { return descriptionAr; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public int getVersion() { return version; }
}
