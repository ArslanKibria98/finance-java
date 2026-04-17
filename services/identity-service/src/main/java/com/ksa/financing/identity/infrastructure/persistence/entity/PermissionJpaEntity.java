package com.ksa.financing.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class PermissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 255)
    private String permissionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "module_id")
    private UUID moduleId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_catalog_visible", nullable = false)
    private boolean catalogVisible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
