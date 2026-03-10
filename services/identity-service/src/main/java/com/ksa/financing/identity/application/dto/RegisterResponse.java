package com.ksa.financing.identity.application.dto;

import java.util.UUID;

public record RegisterResponse(
    UUID userId,
    UUID keycloakUserId,
    String username,
    String status
) {}
