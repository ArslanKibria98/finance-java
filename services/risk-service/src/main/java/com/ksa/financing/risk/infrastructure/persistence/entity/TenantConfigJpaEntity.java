package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_configs")
@Getter
@Setter
public class TenantConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "tenant_name", nullable = false, length = 200)
    private String tenantName;

    @Column(name = "tenant_name_ar", length = 200)
    private String tenantNameAr;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TenantStatusEnum status;

    @Column(name = "customer_risk_enabled", nullable = false)
    private boolean customerRiskEnabled;

    @Column(name = "business_risk_enabled", nullable = false)
    private boolean businessRiskEnabled;

    @Column(name = "loan_risk_enabled", nullable = false)
    private boolean loanRiskEnabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum TenantStatusEnum {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
