package com.ksa.financing.globalprofile.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA entity mapping to the {@code global_customers} table.
 * <p>
 * This entity stores ZERO PII — only SHA-256 hashes of email and mobile
 * for deduplication. The DB trigger {@code compute_global_kyc_status}
 * auto-computes the aggregate KYC status from regional profiles.
 * <p>
 * Note: The DB has a trigger that updates {@code version} and {@code updated_at}
 * on every UPDATE, so after save+flush a refresh is recommended.
 */
@Entity
@Table(name = "global_customers")
@Getter
@Setter
public class GlobalCustomerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "global_uid", updatable = false, nullable = false)
    private UUID globalUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, columnDefinition = "customer_type")
    private CustomerTypeEnum customerType;

    @Column(name = "global_email_hash", nullable = true, length = 64)
    private String globalEmailHash;

    @Column(name = "global_mobile_hash", nullable = false, length = 64)
    private String globalMobileHash;

    @Column(name = "primary_country_code", nullable = false, length = 3)
    private String primaryCountryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "global_kyc_status", nullable = false, columnDefinition = "global_kyc_aggregate_status")
    private GlobalKycAggregateStatusEnum globalKycStatus;

    @Column(name = "kyc_last_verified_at")
    private OffsetDateTime kycLastVerifiedAt;

    @Column(name = "global_risk_grade", length = 10)
    private String globalRiskGrade;

    @Column(name = "global_risk_updated_at")
    private OffsetDateTime globalRiskUpdatedAt;

    @Column(name = "pep_flag", nullable = false)
    private boolean pepFlag;

    @Column(name = "sanctions_flag", nullable = false)
    private boolean sanctionsFlag;

    @Column(name = "fraud_flag", nullable = false)
    private boolean fraudFlag;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "acquisition_source", length = 100)
    private String acquisitionSource;

    @Column(name = "customer_segment", length = 50)
    private String customerSegment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Enum for customer_type PostgreSQL enum.
     */
    public enum CustomerTypeEnum {
        INDIVIDUAL,
        BUSINESS
    }

    /**
     * Enum for global_kyc_aggregate_status PostgreSQL enum.
     */
    public enum GlobalKycAggregateStatusEnum {
        NONE,
        PARTIAL,
        VERIFIED,
        EXPIRED,
        BLOCKED
    }
}
