package com.ksa.financing.product.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Sub-category domain model — pure POJO.
 */
public class SubCategory {

    private UUID id;
    private UUID tenantId;
    private UUID masterCategoryId;
    private String code;
    private String nameEn;
    private String nameAr;
    private int sortOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public SubCategory() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getMasterCategoryId() { return masterCategoryId; }
    public void setMasterCategoryId(UUID masterCategoryId) { this.masterCategoryId = masterCategoryId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
