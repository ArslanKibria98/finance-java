package com.ksa.financing.kycadapter.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
public class OtpVerificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "otp_request_id", nullable = false, unique = true, length = 100)
    private String otpRequestId;

    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "status", nullable = false, columnDefinition = "otp_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private OtpStatus status = OtpStatus.SENT;

    @Column(name = "verify_attempts", nullable = false)
    private int verifyAttempts = 0;

    @Column(name = "max_verify_attempts", nullable = false)
    private int maxVerifyAttempts = 3;

    @Column(name = "resend_count", nullable = false)
    private int resendCount = 0;

    @Column(name = "max_resend_count", nullable = false)
    private int maxResendCount = 3;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum OtpStatus {
        SENT, VERIFIED, EXPIRED, MAX_ATTEMPTS_EXCEEDED, FAILED
    }
}
