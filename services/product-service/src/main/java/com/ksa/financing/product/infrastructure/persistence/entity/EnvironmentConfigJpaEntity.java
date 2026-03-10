package com.ksa.financing.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "environment_configs")
@Getter
@Setter
public class EnvironmentConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "config_code", nullable = false, length = 50)
    private String configCode;

    @Column(name = "configuration_name", nullable = false)
    private String configurationName;

    @Column(name = "configuration_name_ar")
    private String configurationNameAr;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "api_endpoint", length = 1000)
    private String apiEndpoint;

    @Column(name = "parameters", length = 2000)
    private String parameters;

    @Column(name = "credentials", length = 2000)
    private String credentials;

    @Column(name = "headers", length = 2000)
    private String headers;

    @Column(name = "test_mode", nullable = false)
    private boolean testMode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
