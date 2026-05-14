package com.ksa.financing.risk.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MobileBlacklistEntry(
    UUID id,
    String mobileNumber,
    String reason,
    BlacklistStatus status,
    String addedBy,
    UUID blockCodeId,
    String blockCode,
    Instant createdAt,
    Instant updatedAt
) {}
