package com.ksa.financing.customer.infrastructure.persistence.entity;

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

@Entity
@Table(name = "customers")
@Getter
@Setter
// NOTE: Filename should be CustomerJpaEntity.java - renamed class per naming conventions
public class CustomerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "cif_number", nullable = false, length = 20)
    private String cifNumber;

    @Column(name = "customer_type", nullable = false)
    private String customerType;

    @Column(name = "national_id", nullable = false, length = 255)
    private String nationalId;

    @Column(name = "national_id_type", nullable = false, length = 20)
    private String nationalIdType;

    @Column(name = "title", length = 20)
    private String title;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name_ar", length = 100)
    private String firstNameAr;

    @Column(name = "last_name_ar", length = 100)
    private String lastNameAr;

    @Column(name = "full_name", nullable = false, length = 500)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "nationality", nullable = false, length = 3)
    private String nationality;

    @Column(name = "residency_type", nullable = false)
    private String residencyType;

    @Column(name = "mobile_number", nullable = false, length = 255)
    private String mobileNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 3)
    private String country;

    @Column(name = "kyc_status", nullable = false)
    private String kycStatus;

    @Column(name = "kyc_verified_at")
    private OffsetDateTime kycVerifiedAt;

    @Column(name = "kyc_expiry_date")
    private LocalDate kycExpiryDate;

    @Column(name = "nafath_verified", nullable = false)
    private boolean nafathVerified;

    @Column(name = "nafath_transaction_id", length = 100)
    private String nafathTransactionId;

    @Column(name = "risk_grade")
    private String riskGrade;

    @Column(name = "risk_grade_updated_at")
    private OffsetDateTime riskGradeUpdatedAt;

    @Column(name = "pep_flag", nullable = false)
    private boolean pepFlag;

    @Column(name = "sanctions_flag", nullable = false)
    private boolean sanctionsFlag;

    @Column(name = "keycloak_user_id")
    private UUID keycloakUserId;

    @Column(name = "global_uid")
    private UUID globalUid;

    @Column(name = "lifecycle_stage", nullable = false)
    private String lifecycleStage;

    @Column(name = "lifecycle_stage_changed_at", nullable = false)
    private OffsetDateTime lifecycleStageChangedAt;

    @Column(name = "customer_segment", length = 50)
    private String customerSegment;

    @Column(name = "acquisition_channel", length = 50)
    private String acquisitionChannel;

    @Column(name = "acquisition_partner_id")
    private UUID acquisitionPartnerId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "blocked_at")
    private OffsetDateTime blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
