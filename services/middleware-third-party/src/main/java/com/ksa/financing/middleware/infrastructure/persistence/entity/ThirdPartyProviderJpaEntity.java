package com.ksa.financing.middleware.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "third_party_providers")
@Getter
@Setter
public class ThirdPartyProviderJpaEntity {

    public enum ProviderStatusEnum { ACTIVE, INACTIVE, MAINTENANCE, DEPRECATED }
    public enum AuthTypeEnum { NONE, BASIC, BEARER, API_KEY, OAUTH2, CUSTOM }
    public enum ProviderCategoryEnum {
        IDENTITY, CREDIT_BUREAU, GOVERNMENT, COMMUNICATION,
        OPEN_BANKING, COMMODITY_TRADING, PROMISSORY_NOTES,
        DIGITAL_SIGNING, NOTIFICATIONS, BANKING, PAYMENT_GATEWAY,
        INCOME_VERIFICATION, FINANCIAL, AML_SCREENING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, columnDefinition = "provider_category")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ProviderCategoryEnum category;

    @Column(name = "base_url_dev", columnDefinition = "TEXT")
    private String baseUrlDev;

    @Column(name = "base_url_prod", columnDefinition = "TEXT")
    private String baseUrlProd;

    @Column(name = "auth_type", nullable = false, columnDefinition = "auth_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private AuthTypeEnum authType;

    @Column(name = "status", nullable = false, columnDefinition = "provider_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ProviderStatusEnum status;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

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
