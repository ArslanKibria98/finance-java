package com.ksa.financing.middleware.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_environment_configs")
@Getter
@Setter
public class ApiEnvironmentConfigJpaEntity {

    public enum EnvironmentTypeEnum { DEV, PROD }
    public enum AuthTypeEnum { NONE, BASIC, BEARER, API_KEY, OAUTH2, CUSTOM }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "api_id", nullable = false)
    private UUID apiId;

    @Column(name = "environment", nullable = false, columnDefinition = "environment_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EnvironmentTypeEnum environment;

    @Column(name = "base_url", nullable = false, columnDefinition = "TEXT")
    private String baseUrl;

    @Column(name = "endpoint_path", columnDefinition = "TEXT")
    private String endpointPath;

    @Column(name = "credentials", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String credentials;

    @Column(name = "headers", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String headers;

    @Column(name = "query_params", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String queryParams;

    @Column(name = "auth_type", columnDefinition = "auth_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private AuthTypeEnum authType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

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
}
