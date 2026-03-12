package com.ksa.financing.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code products} table.
 * Enums stored as VARCHAR strings (no @Enumerated).
 */
@Entity
@Table(name = "products")
@Getter
@Setter
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // --- Product identification ---

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_ar")
    private String nameAr;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_ar", columnDefinition = "TEXT")
    private String descriptionAr;

    @Column(name = "short_description_en", columnDefinition = "TEXT")
    private String shortDescriptionEn;

    @Column(name = "short_description_ar", columnDefinition = "TEXT")
    private String shortDescriptionAr;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // --- Classification ---

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @Column(name = "target_segment", nullable = false, length = 50)
    private String targetSegment;

    @Column(name = "master_category_id")
    private UUID masterCategoryId;

    @Column(name = "sub_category_id")
    private UUID subCategoryId;

    @Column(name = "template_id")
    private UUID templateId;

    // --- UI fields ---

    @Column(name = "notification_email")
    private String notificationEmail;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "customer_types", columnDefinition = "VARCHAR(50)[]")
    private String[] customerTypes;

    @Column(name = "involves_commodity", nullable = false)
    private boolean involvesCommodity;

    @Column(name = "setup_method", length = 20)
    private String setupMethod;

    // --- Wizard progress ---

    @Column(name = "wizard_step", nullable = false)
    private int wizardStep;

    @Column(name = "wizard_completed", nullable = false)
    private boolean wizardCompleted;

    // --- Sharia configuration ---

    @Column(name = "sharia_structure", nullable = false, length = 50)
    private String shariaStructure;

    @Column(name = "commodity_required", nullable = false)
    private boolean commodityRequired;

    // --- Availability ---

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "visible_to_customers", nullable = false)
    private boolean visibleToCustomers;

    @Column(name = "visible_to_partners", nullable = false)
    private boolean visibleToPartners;

    // --- Financial terms ---

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "min_tenure_months", nullable = false)
    private int minTenureMonths;

    @Column(name = "max_tenure_months", nullable = false)
    private int maxTenureMonths;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_tenures", columnDefinition = "INT[]")
    private Integer[] allowedTenures;

    // --- Profit rate ---

    @Column(name = "base_profit_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal baseProfitRate;

    @Column(name = "rate_type", nullable = false, length = 20)
    private String rateType;

    // --- Repayment ---

    @Column(name = "repayment_frequency", nullable = false, length = 20)
    private String repaymentFrequency;

    @Column(name = "grace_period_days", nullable = false)
    private int gracePeriodDays;

    // --- Early settlement ---

    @Column(name = "early_settlement_allowed", nullable = false)
    private boolean earlySettlementAllowed;

    @Column(name = "waive_unearned_profit", nullable = false)
    private boolean waiveUnearnedProfit;

    @Column(name = "min_tenure_before_settlement")
    private Integer minTenureBeforeSettlement;

    // --- Core banking ---

    @Column(name = "fineract_product_id", length = 50)
    private String fineractProductId;

    // --- Regional ---

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "country_id")
    private UUID countryId;

    // --- Audit ---

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
