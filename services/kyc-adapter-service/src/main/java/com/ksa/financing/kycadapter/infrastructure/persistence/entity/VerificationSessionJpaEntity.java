package com.ksa.financing.kycadapter.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to the verification_sessions table.
 * Stores KYC verification session data with multi-provider and multi-tenant support.
 */
@Entity
@Table(name = "verification_sessions")
@Getter
@Setter
public class VerificationSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "session_number", nullable = false, length = 50)
    private String sessionNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "global_uid")
    private UUID globalUid;

    @Column(name = "loan_application_id")
    private UUID loanApplicationId;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode = "SAU";

    @Column(name = "verification_type", nullable = false, columnDefinition = "verification_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private VerificationTypeEnum verificationType;

    @Column(name = "provider", nullable = false, columnDefinition = "kyc_provider")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private KycProviderEnum provider;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "iqama_number", length = 20)
    private String iqamaNumber;

    @Column(name = "commercial_registration", length = 50)
    private String commercialRegistration;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "full_name_ar", length = 255)
    private String fullNameAr;

    @Column(name = "full_name_en", length = 255)
    private String fullNameEn;

    @Column(name = "status", nullable = false, columnDefinition = "session_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private SessionStatusEnum status;

    @Column(name = "provider_session_id", length = 100)
    private String providerSessionId;

    @Column(name = "provider_request_id", length = 100)
    private String providerRequestId;

    @Column(name = "result", columnDefinition = "verification_result")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private VerificationResultEnum result;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "initiated_at", nullable = false)
    private OffsetDateTime initiatedAt;

    @Column(name = "user_action_at")
    private OffsetDateTime userActionAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 1;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (initiatedAt == null) {
            initiatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * JPA-local enum mirroring the PostgreSQL kyc_provider enum type.
     */
    public enum KycProviderEnum {
        YAKEEN, NAFATH, ABSHER, NIC, UAE_PASS, UAE_ICA, NADRA, MANUAL
    }

    /**
     * JPA-local enum mirroring the PostgreSQL verification_type enum type.
     */
    public enum VerificationTypeEnum {
        NATIONAL_ID, IQAMA, COMMERCIAL_REGISTRATION, PASSPORT, LIVENESS, ADDRESS, EMPLOYMENT
    }

    /**
     * JPA-local enum mirroring the PostgreSQL session_status enum type.
     */
    public enum SessionStatusEnum {
        INITIATED, PENDING_USER_ACTION, IN_PROGRESS, COMPLETED, FAILED, EXPIRED, CANCELLED
    }

    /**
     * JPA-local enum mirroring the PostgreSQL verification_result enum type.
     */
    public enum VerificationResultEnum {
        VERIFIED, NOT_VERIFIED, PARTIAL_MATCH, DATA_MISMATCH, EXPIRED_DOCUMENT, PROVIDER_ERROR
    }
}
