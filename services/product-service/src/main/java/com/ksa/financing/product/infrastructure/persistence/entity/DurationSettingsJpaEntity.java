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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code product_duration_settings} table.
 */
@Entity
@Table(name = "product_duration_settings")
@Getter
@Setter
public class DurationSettingsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "request_duration_days", nullable = false)
    private int requestDurationDays;

    @Column(name = "approval_duration_days", nullable = false)
    private int approvalDurationDays;

    @Column(name = "disbursement_duration_days", nullable = false)
    private int disbursementDurationDays;

    @Column(name = "repayment_duration_days", nullable = false)
    private int repaymentDurationDays;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
