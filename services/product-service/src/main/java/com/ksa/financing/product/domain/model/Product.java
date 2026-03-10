package com.ksa.financing.product.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Product aggregate root — pure domain model with zero framework imports.
 */
public class Product {

    private UUID id;
    private UUID tenantId;

    // Identification
    private String productCode;
    private String nameEn;
    private String nameAr;
    private String descriptionEn;
    private String descriptionAr;
    private String shortDescriptionEn;
    private String shortDescriptionAr;
    private String logoUrl;

    // Classification
    private ProductType productType;
    private String targetSegment;
    private UUID masterCategoryId;
    private UUID subCategoryId;
    private UUID templateId;

    // UI fields
    private String notificationEmail;
    private List<String> customerTypes;
    private boolean involvesCommodity;
    private String setupMethod;

    // Wizard progress
    private int wizardStep;
    private boolean wizardCompleted;

    // Sharia configuration
    private String shariaStructure;
    private boolean commodityRequired;

    // Availability
    private ProductStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean visibleToCustomers;
    private boolean visibleToPartners;

    // Financial terms
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int minTenureMonths;
    private int maxTenureMonths;
    private List<Integer> allowedTenures;

    // Profit rate
    private BigDecimal baseProfitRate;
    private String rateType;

    // Repayment
    private String repaymentFrequency;
    private int gracePeriodDays;

    // Early settlement
    private boolean earlySettlementAllowed;
    private boolean waiveUnearnedProfit;
    private Integer minTenureBeforeSettlement;

    // Regional
    private String currency;

    // Admin fee slabs (loaded eagerly on getById)
    private List<AdminFeeSlab> adminFeeSlabs;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private int versionNumber;
    private int version;
    private Instant deletedAt;

    public Product() {}

    // === Factory methods ===

    public static Product create(UUID tenantId, String productCode, String nameEn,
                                  ProductType productType, String shariaStructure,
                                  BigDecimal minAmount, BigDecimal maxAmount,
                                  Integer minTenureMonths, Integer maxTenureMonths,
                                  BigDecimal baseProfitRate) {
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Min amount must be positive");
        }
        if (minAmount != null && maxAmount != null && maxAmount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("Max amount must be >= min amount");
        }
        if (minTenureMonths != null && minTenureMonths <= 0) {
            throw new IllegalArgumentException("Min tenure months must be positive");
        }
        if (maxTenureMonths != null && minTenureMonths != null && maxTenureMonths < minTenureMonths) {
            throw new IllegalArgumentException("Max tenure must be >= min tenure");
        }

        var product = new Product();
        product.tenantId = tenantId;
        product.productCode = productCode;
        product.nameEn = nameEn;
        product.productType = productType;
        product.shariaStructure = shariaStructure;
        product.minAmount = minAmount;
        product.maxAmount = maxAmount;
        product.minTenureMonths = minTenureMonths != null ? minTenureMonths : 0;
        product.maxTenureMonths = maxTenureMonths != null ? maxTenureMonths : 0;
        product.baseProfitRate = baseProfitRate;
        product.status = ProductStatus.DRAFT;
        product.wizardStep = 1;
        product.wizardCompleted = false;
        product.targetSegment = "INDIVIDUAL";
        product.rateType = "REDUCING_BALANCE";
        product.repaymentFrequency = "MONTHLY";
        product.gracePeriodDays = 3;
        product.earlySettlementAllowed = true;
        product.waiveUnearnedProfit = true;
        product.minTenureBeforeSettlement = 3;
        product.currency = "SAR";
        product.versionNumber = 1;
        product.version = 1;
        product.createdAt = Instant.now();
        product.updatedAt = Instant.now();
        return product;
    }

    // === Domain behavior ===

    public void activate() {
        if (this.status != ProductStatus.DRAFT && this.status != ProductStatus.INACTIVE) {
            throw new IllegalStateException(
                "Cannot activate product in status: " + this.status);
        }
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status != ProductStatus.ACTIVE) {
            throw new IllegalStateException(
                "Cannot deactivate product in status: " + this.status);
        }
        this.status = ProductStatus.INACTIVE;
    }

    public void archive() {
        if (this.status == ProductStatus.ARCHIVED) {
            throw new IllegalStateException("Product is already archived");
        }
        this.status = ProductStatus.ARCHIVED;
    }

    public void advanceWizardStep(int step) {
        if (step < 1 || step > 5) {
            throw new IllegalArgumentException("Wizard step must be between 1 and 5");
        }
        this.wizardStep = step;
        if (step == 5) {
            this.wizardCompleted = true;
        }
    }

    // === Getters and Setters ===

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }

    public String getDescriptionAr() { return descriptionAr; }
    public void setDescriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; }

    public String getShortDescriptionEn() { return shortDescriptionEn; }
    public void setShortDescriptionEn(String shortDescriptionEn) { this.shortDescriptionEn = shortDescriptionEn; }

    public String getShortDescriptionAr() { return shortDescriptionAr; }
    public void setShortDescriptionAr(String shortDescriptionAr) { this.shortDescriptionAr = shortDescriptionAr; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public String getTargetSegment() { return targetSegment; }
    public void setTargetSegment(String targetSegment) { this.targetSegment = targetSegment; }

    public UUID getMasterCategoryId() { return masterCategoryId; }
    public void setMasterCategoryId(UUID masterCategoryId) { this.masterCategoryId = masterCategoryId; }

    public UUID getSubCategoryId() { return subCategoryId; }
    public void setSubCategoryId(UUID subCategoryId) { this.subCategoryId = subCategoryId; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }

    public List<String> getCustomerTypes() { return customerTypes; }
    public void setCustomerTypes(List<String> customerTypes) { this.customerTypes = customerTypes; }

    public boolean isInvolvesCommodity() { return involvesCommodity; }
    public void setInvolvesCommodity(boolean involvesCommodity) { this.involvesCommodity = involvesCommodity; }

    public String getSetupMethod() { return setupMethod; }
    public void setSetupMethod(String setupMethod) { this.setupMethod = setupMethod; }

    public int getWizardStep() { return wizardStep; }
    public void setWizardStep(int wizardStep) { this.wizardStep = wizardStep; }

    public boolean isWizardCompleted() { return wizardCompleted; }
    public void setWizardCompleted(boolean wizardCompleted) { this.wizardCompleted = wizardCompleted; }

    public String getShariaStructure() { return shariaStructure; }
    public void setShariaStructure(String shariaStructure) { this.shariaStructure = shariaStructure; }

    public boolean isCommodityRequired() { return commodityRequired; }
    public void setCommodityRequired(boolean commodityRequired) { this.commodityRequired = commodityRequired; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isVisibleToCustomers() { return visibleToCustomers; }
    public void setVisibleToCustomers(boolean visibleToCustomers) { this.visibleToCustomers = visibleToCustomers; }

    public boolean isVisibleToPartners() { return visibleToPartners; }
    public void setVisibleToPartners(boolean visibleToPartners) { this.visibleToPartners = visibleToPartners; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public int getMinTenureMonths() { return minTenureMonths; }
    public void setMinTenureMonths(int minTenureMonths) { this.minTenureMonths = minTenureMonths; }

    public int getMaxTenureMonths() { return maxTenureMonths; }
    public void setMaxTenureMonths(int maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; }

    public List<Integer> getAllowedTenures() { return allowedTenures; }
    public void setAllowedTenures(List<Integer> allowedTenures) { this.allowedTenures = allowedTenures; }

    public BigDecimal getBaseProfitRate() { return baseProfitRate; }
    public void setBaseProfitRate(BigDecimal baseProfitRate) { this.baseProfitRate = baseProfitRate; }

    public String getRateType() { return rateType; }
    public void setRateType(String rateType) { this.rateType = rateType; }

    public String getRepaymentFrequency() { return repaymentFrequency; }
    public void setRepaymentFrequency(String repaymentFrequency) { this.repaymentFrequency = repaymentFrequency; }

    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }

    public boolean isEarlySettlementAllowed() { return earlySettlementAllowed; }
    public void setEarlySettlementAllowed(boolean earlySettlementAllowed) { this.earlySettlementAllowed = earlySettlementAllowed; }

    public boolean isWaiveUnearnedProfit() { return waiveUnearnedProfit; }
    public void setWaiveUnearnedProfit(boolean waiveUnearnedProfit) { this.waiveUnearnedProfit = waiveUnearnedProfit; }

    public Integer getMinTenureBeforeSettlement() { return minTenureBeforeSettlement; }
    public void setMinTenureBeforeSettlement(Integer minTenureBeforeSettlement) { this.minTenureBeforeSettlement = minTenureBeforeSettlement; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public List<AdminFeeSlab> getAdminFeeSlabs() { return adminFeeSlabs; }
    public void setAdminFeeSlabs(List<AdminFeeSlab> adminFeeSlabs) { this.adminFeeSlabs = adminFeeSlabs; }
}
