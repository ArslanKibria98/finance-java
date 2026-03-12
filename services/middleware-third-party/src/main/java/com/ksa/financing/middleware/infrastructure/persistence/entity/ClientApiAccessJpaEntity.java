package com.ksa.financing.middleware.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_api_access")
@Getter
@Setter
public class ClientApiAccessJpaEntity {

    public enum AccessEnvironmentEnum { DEV, PROD, BOTH, TEST }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "api_id", nullable = false)
    private UUID apiId;

    @Column(name = "environment", nullable = false, columnDefinition = "access_environment")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private AccessEnvironmentEnum environment;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private OffsetDateTime grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) grantedAt = OffsetDateTime.now();
    }
}
