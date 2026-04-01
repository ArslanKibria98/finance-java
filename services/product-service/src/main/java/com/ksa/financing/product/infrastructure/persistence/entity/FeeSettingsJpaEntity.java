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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code product_fee_settings} table.
 */
@Entity
@Table(name = "product_fee_settings")
@Getter
@Setter
public class FeeSettingsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "revenue_eligibility_threshold", precision = 19, scale = 4)
    private BigDecimal revenueEligibilityThreshold;

    @Column(name = "max_dbr_percentage", precision = 5, scale = 2)
    private BigDecimal maxDbrPercentage;

    @Column(name = "global_dbr_percentage", precision = 5, scale = 2)
    private BigDecimal globalDbrPercentage;

    @Column(name = "dbr_calculation_method", length = 50)
    private String dbrCalculationMethod;

    @Column(name = "dbr_exceptions", columnDefinition = "TEXT")
    private String dbrExceptions;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
