package com.ksa.financing.product.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Country domain model — pure POJO.
 */
public class Country {

    private UUID id;
    private UUID tenantId;
    private String code;
    private String nameEn;
    private String nameAr;
    private String dialCode;
    private String currencyCode;
    private boolean gcc;
    private boolean active;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public Country() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getDialCode() { return dialCode; }
    public void setDialCode(String dialCode) { this.dialCode = dialCode; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public boolean isGcc() { return gcc; }
    public void setGcc(boolean gcc) { this.gcc = gcc; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
