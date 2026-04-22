package com.ksa.financing.ledger.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CoaConfigurationProfile {

    private final UUID id;
    private final UUID tenantId;
    private final String productCode;
    private String profileName;
    private CoaConfigurationStatus status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    private CoaConfigurationProfile(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.productCode = builder.productCode;
        this.profileName = builder.profileName;
        this.status = builder.status;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.version = builder.version;
    }

    public static CoaConfigurationProfile create(UUID tenantId, String productCode, String profileName) {
        if (tenantId == null) throw new IllegalArgumentException("TenantId cannot be null");
        if (productCode == null || productCode.isBlank()) throw new IllegalArgumentException("Product code cannot be blank");
        if (profileName == null || profileName.isBlank()) throw new IllegalArgumentException("Profile name cannot be blank");

        var now = LocalDateTime.now();
        return new Builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productCode(productCode.trim().toUpperCase())
                .profileName(profileName.trim())
                .status(CoaConfigurationStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .version(1)
                .build();
    }

    public static CoaConfigurationProfile reconstitute(Builder builder) {
        return new CoaConfigurationProfile(builder);
    }

    public void updateProfile(String profileName, LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (profileName == null || profileName.isBlank()) throw new IllegalArgumentException("Profile name cannot be blank");
        this.profileName = profileName.trim();
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = CoaConfigurationStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = CoaConfigurationStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getProductCode() { return productCode; }
    public String getProfileName() { return profileName; }
    public CoaConfigurationStatus getStatus() { return status; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID tenantId;
        private String productCode;
        private String profileName;
        private CoaConfigurationStatus status;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private int version;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder productCode(String productCode) { this.productCode = productCode; return this; }
        public Builder profileName(String profileName) { this.profileName = profileName; return this; }
        public Builder status(CoaConfigurationStatus status) { this.status = status; return this; }
        public Builder effectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; return this; }
        public Builder effectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public CoaConfigurationProfile build() { return new CoaConfigurationProfile(this); }
    }
}
