package com.ksa.financing.identity.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record PermissionResponse(
    UUID id,
    UUID tenantId,
    String permissionCode,
    String permissionName,
    String description,
    String resourceType,
    UUID moduleId,
    String action,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
