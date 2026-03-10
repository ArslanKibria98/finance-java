package com.ksa.financing.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class RoleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 255)
    private String roleName;

    @Column(name = "role_name_ar", length = 255)
    private String roleNameAr;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "keycloak_role_id")
    private UUID keycloakRoleId;

    @Column(name = "keycloak_realm", length = 100)
    private String keycloakRealm;

    @Column(name = "parent_role_id")
    private UUID parentRoleId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 1;

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
