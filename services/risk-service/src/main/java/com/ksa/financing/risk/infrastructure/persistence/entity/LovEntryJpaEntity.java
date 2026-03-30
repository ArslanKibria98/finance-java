package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lov_entries")
@Getter
@Setter
public class LovEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lov_set_id", nullable = false)
    private UUID lovSetId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "factor_code", nullable = false, length = 100)
    private String factorCode;

    @Column(name = "label_en", nullable = false, length = 300)
    private String labelEn;

    @Column(name = "label_ar", length = 300)
    private String labelAr;

    @Column(name = "factor_weight", nullable = false, precision = 10, scale = 4)
    private BigDecimal factorWeight;

    @Column(name = "risk_status", length = 20)
    private String riskStatus;

    @Column(name = "lov_version", nullable = false)
    private int lovVersion;

    @Column(name = "active", nullable = false)
    private boolean active;

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
