package com.ksa.financing.identity.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
    UUID id,
    UUID tenantId,
    UUID keycloakUserId,
    String name,
    String email,
    String phone,
    String address,
    UUID roleId,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
