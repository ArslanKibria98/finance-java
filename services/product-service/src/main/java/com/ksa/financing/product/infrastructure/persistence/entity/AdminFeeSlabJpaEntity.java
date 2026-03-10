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

@Entity
@Table(name = "product_admin_fee_slabs")
@Getter
@Setter
public class AdminFeeSlabJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "profit_percentage", precision = 7, scale = 4)
    private BigDecimal profitPercentage;

    @Column(name = "processing_fee", precision = 19, scale = 4)
    private BigDecimal processingFee;

    @Column(name = "admin_fee", precision = 19, scale = 4)
    private BigDecimal adminFee;

    @Column(name = "partner_scope", nullable = false, length = 50)
    private String partnerScope;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "min_tenure")
    private Integer minTenure;

    @Column(name = "max_tenure")
    private Integer maxTenure;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
