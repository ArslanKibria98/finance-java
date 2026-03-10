package com.ksa.financing.risk.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MobileBlacklistEntry(
    UUID id,
    String mobileNumber,
    String reason,
    BlacklistStatus status,
    String addedBy,
    Instant createdAt,
    Instant updatedAt
) {}
