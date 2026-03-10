package com.ksa.financing.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Role {
    private UUID id;
    private UUID tenantId;
    private String roleCode;
    private String roleName;
    private String roleNameAr;
    private String description;
    private UUID keycloakRoleId;
    private String keycloakRealm;
    private UUID parentRoleId;
    private boolean active;
    private boolean system;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleNameAr() { return roleNameAr; }
    public void setRoleNameAr(String roleNameAr) { this.roleNameAr = roleNameAr; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getKeycloakRoleId() { return keycloakRoleId; }
    public void setKeycloakRoleId(UUID keycloakRoleId) { this.keycloakRoleId = keycloakRoleId; }
    public String getKeycloakRealm() { return keycloakRealm; }
    public void setKeycloakRealm(String keycloakRealm) { this.keycloakRealm = keycloakRealm; }
    public UUID getParentRoleId() { return parentRoleId; }
    public void setParentRoleId(UUID parentRoleId) { this.parentRoleId = parentRoleId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
