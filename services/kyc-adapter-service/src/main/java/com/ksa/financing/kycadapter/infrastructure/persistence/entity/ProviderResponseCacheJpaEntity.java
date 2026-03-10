package com.ksa.financing.kycadapter.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to the provider_response_cache table.
 * Caches provider responses with TTL for cost optimization and rate-limit management.
 */
@Entity
@Table(name = "provider_response_cache")
@Getter
@Setter
public class ProviderResponseCacheJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "cache_key", nullable = false, length = 100)
    private String cacheKey;

    @Column(name = "provider", nullable = false, columnDefinition = "kyc_provider")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private VerificationSessionJpaEntity.KycProviderEnum provider;

    @Column(name = "verification_type", nullable = false, columnDefinition = "verification_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private VerificationSessionJpaEntity.VerificationTypeEnum verificationType;

    @Column(name = "subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "subject_id_type", nullable = false, length = 20)
    private String subjectIdType;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "request_payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requestPayload;

    @Column(name = "response_payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responsePayload;

    @Column(name = "response_code", length = 20)
    private String responseCode;

    @Column(name = "result", nullable = false, columnDefinition = "verification_result")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private VerificationSessionJpaEntity.VerificationResultEnum result;

    @Column(name = "extracted_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String extractedData;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    @Column(name = "is_stale", nullable = false)
    private boolean isStale = false;

    @Column(name = "hit_count", nullable = false)
    private int hitCount = 0;

    @Column(name = "last_hit_at")
    private OffsetDateTime lastHitAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (fetchedAt == null) {
            fetchedAt = now;
        }
    }
}
