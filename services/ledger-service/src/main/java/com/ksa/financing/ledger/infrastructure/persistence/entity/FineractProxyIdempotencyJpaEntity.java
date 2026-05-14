package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotency cache entry for Fineract proxy calls.
 * Backs the {@code fineract_proxy_idempotency} table (V19 migration).
 */
@Entity
@Table(name = "fineract_proxy_idempotency")
@IdClass(FineractProxyIdempotencyJpaEntity.PK.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineractProxyIdempotencyJpaEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cached_response", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> cachedResponse;

    @Column(name = "cached_status", nullable = false)
    private Integer cachedStatus;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private UUID tenantId;
        private String idempotencyKey;
    }
}
