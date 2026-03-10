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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code regional_profiles} table.
 * <p>
 * Links a global customer to a regional CIF (Customer Information File)
 * with PII vault references. Each global customer has at most one profile
 * per country (enforced by UNIQUE constraint on global_uid + country_code).
 */
@Entity
@Table(name = "regional_profiles")
@Getter
@Setter
public class RegionalProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "regional_profile_id", updatable = false, nullable = false)
    private UUID regionalProfileId;

    @Column(name = "global_uid", nullable = false)
    private UUID globalUid;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "regional_cif_number", nullable = false, length = 20)
    private String regionalCifNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "regional_kyc_status", nullable = false, columnDefinition = "kyc_status")
    private KycStatusEnum regionalKycStatus;

    @Column(name = "kyc_verified_at")
    private OffsetDateTime kycVerifiedAt;

    @Column(name = "kyc_expiry_date")
    private LocalDate kycExpiryDate;

    @Column(name = "kyc_provider", length = 50)
    private String kycProvider;

    @Column(name = "pii_vault_region", nullable = false, length = 10)
    private String piiVaultRegion;

    @Column(name = "pii_vault_record_id", nullable = false)
    private UUID piiVaultRecordId;

    @Column(name = "regional_risk_grade", length = 10)
    private String regionalRiskGrade;

    @Column(name = "risk_grade_updated_at")
    private OffsetDateTime riskGradeUpdatedAt;

    @Column(name = "regional_pep_flag", nullable = false)
    private boolean regionalPepFlag;

    @Column(name = "regional_sanctions_flag", nullable = false)
    private boolean regionalSanctionsFlag;

    @Column(name = "keycloak_user_id")
    private UUID keycloakUserId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "deactivation_date")
    private LocalDate deactivationDate;

    @Column(name = "deactivation_reason", length = 500)
    private String deactivationReason;

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
     * Enum for kyc_status PostgreSQL enum.
     */
    public enum KycStatusEnum {
        NOT_STARTED,
        IN_PROGRESS,
        PENDING_REVIEW,
        VERIFIED,
        EXPIRED,
        REJECTED,
        BLOCKED
    }
}
