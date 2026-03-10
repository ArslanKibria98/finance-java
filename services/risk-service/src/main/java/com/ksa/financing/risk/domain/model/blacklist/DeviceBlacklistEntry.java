package com.ksa.financing.risk.domain.model.blacklist;

import com.ksa.financing.risk.domain.model.fraud.FraudBlockType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceBlacklistEntry(
    UUID id,
    UUID tenantId,
    String deviceId,
    String reason,
    FraudBlockType blockType,
    int escalationCount,
    boolean active,
    UUID addedBy,
    LocalDateTime addedAt,
    LocalDateTime expiresAt
) {}
