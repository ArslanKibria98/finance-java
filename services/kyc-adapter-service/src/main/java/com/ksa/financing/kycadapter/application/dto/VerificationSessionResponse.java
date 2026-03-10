package com.ksa.financing.kycadapter.application.dto;

import java.time.Instant;
import java.util.UUID;

public record VerificationSessionResponse(
    UUID id,
    String sessionNumber,
    String verificationType,
    String provider,
    String status,
    String result,
    Instant initiatedAt,
    Instant completedAt
) {}
