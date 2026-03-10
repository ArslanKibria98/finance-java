package com.ksa.financing.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserIdentityResponse(
    UUID id,
    UUID keycloakUserId,
    String keycloakUsername,
    String userType,
    String status,
    UUID globalUid,
    Instant createdAt
) {}
