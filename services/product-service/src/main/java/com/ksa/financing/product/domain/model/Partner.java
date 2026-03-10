package com.ksa.financing.product.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Partner domain model — pure domain with zero framework imports.
 */
public class Partner {

    private UUID id;
    private UUID tenantId;

    private String partnerCode;
    private String nameEn;
    private String nameAr;
    private String email;
    private String phone;
    private String contactPerson;
    private String logoUrl;
    private String status; // ACTIVE, INACTIVE, SUSPENDED

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private int version;

    public Partner() {}

    // === Factory method ===

    public static Partner create(UUID tenantId, String partnerCode, String nameEn, UUID createdBy) {
        if (partnerCode == null || partnerCode.isBlank()) {
            throw new IllegalArgumentException("Partner code is required");
        }
        if (nameEn == null || nameEn.isBlank()) {
            throw new IllegalArgumentException("Partner name (English) is required");
        }

        var partner = new Partner();
        partner.tenantId = tenantId;
        partner.partnerCode = partnerCode;
        partner.nameEn = nameEn;
        partner.status = "ACTIVE";
        partner.createdBy = createdBy;
        partner.version = 1;
        partner.createdAt = Instant.now();
        partner.updatedAt = Instant.now();
        return partner;
    }

    // === Domain behavior ===

    public void suspend() {
        if (!"ACTIVE".equals(this.status)) {
            throw new IllegalStateException("Can only suspend an ACTIVE partner, current: " + this.status);
        }
        this.status = "SUSPENDED";
    }

    public void activate() {
        if ("ACTIVE".equals(this.status)) {
            throw new IllegalStateException("Partner is already ACTIVE");
        }
        this.status = "ACTIVE";
    }

    public void deactivate() {
        if ("INACTIVE".equals(this.status)) {
            throw new IllegalStateException("Partner is already INACTIVE");
        }
        this.status = "INACTIVE";
    }

    // === Getters and Setters ===

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
