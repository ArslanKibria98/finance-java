package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log row for every Fineract proxy call.
 * Backs the {@code fineract_audit_log} table (V18 migration).
 */
@Entity
@Table(name = "fineract_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineractAuditLogJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "caller_service", nullable = false, length = 50)
    private String callerService;

    @Column(name = "caller_user_id", length = 100)
    private String callerUserId;

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "fineract_endpoint", nullable = false, length = 255)
    private String fineractEndpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", columnDefinition = "jsonb")
    private Map<String, Object> requestBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "fineract_resource_id", length = 100)
    private String fineractResourceId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;
}
