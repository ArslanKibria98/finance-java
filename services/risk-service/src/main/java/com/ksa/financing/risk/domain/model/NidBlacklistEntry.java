package com.ksa.financing.risk.domain.model;

import com.ksa.financing.domain.valueobject.NationalId;

import java.time.Instant;
import java.util.UUID;

public record NidBlacklistEntry(
    UUID id,
    NationalId nationalId,
    String reason,
    BlacklistStatus status,
    String addedBy,
    UUID blockCodeId,
    String blockCode,
    Instant createdAt,
    Instant updatedAt
) {}
