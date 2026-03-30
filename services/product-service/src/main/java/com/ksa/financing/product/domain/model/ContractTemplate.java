package com.ksa.financing.product.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ContractTemplate {

    private UUID id;
    private UUID tenantId;
    private String nameEn;
    private String nameAr;
    private UUID productId;
    private UUID typeId;
    private String language;
    private String message;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Transient fields for response enrichment
    private String productNameEn;
    private String productNameAr;
    private String typeNameEn;
    private String typeNameAr;

    public ContractTemplate() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public UUID getTypeId() { return typeId; }
    public void setTypeId(UUID typeId) { this.typeId = typeId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getProductNameEn() { return productNameEn; }
    public void setProductNameEn(String productNameEn) { this.productNameEn = productNameEn; }

    public String getProductNameAr() { return productNameAr; }
    public void setProductNameAr(String productNameAr) { this.productNameAr = productNameAr; }

    public String getTypeNameEn() { return typeNameEn; }
    public void setTypeNameEn(String typeNameEn) { this.typeNameEn = typeNameEn; }

    public String getTypeNameAr() { return typeNameAr; }
    public void setTypeNameAr(String typeNameAr) { this.typeNameAr = typeNameAr; }
}
