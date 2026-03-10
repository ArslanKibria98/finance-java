package com.ksa.financing.middleware.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_request_logs")
@Getter
@Setter
public class ApiRequestLogJpaEntity {

    public enum EnvironmentTypeEnum { DEV, PROD }
    public enum HttpMethodEnum { GET, POST, PUT, PATCH, DELETE }
    public enum RequestStatusEnum { PENDING, SUCCESS, FAILED, TIMEOUT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "api_id", nullable = false)
    private UUID apiId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "environment", nullable = false, columnDefinition = "environment_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EnvironmentTypeEnum environment;

    @Column(name = "http_method", nullable = false, columnDefinition = "http_method")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private HttpMethodEnum httpMethod;

    @Column(name = "request_url", nullable = false, columnDefinition = "TEXT")
    private String requestUrl;

    @Column(name = "request_headers", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_headers", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseBody;

    @Column(name = "status", nullable = false, columnDefinition = "request_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private RequestStatusEnum status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
