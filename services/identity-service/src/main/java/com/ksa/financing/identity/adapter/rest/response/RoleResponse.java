package com.ksa.financing.identity.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    UUID tenantId,
    String roleCode,
    String roleName,
    String roleNameAr,
    String description,
    boolean active,
    boolean system,
    Instant createdAt,
    Instant updatedAt,
    int version
) {}
