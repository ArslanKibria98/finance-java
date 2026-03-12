package com.ksa.financing.fraud.domain.model.blacklist;

import java.time.LocalDateTime;
import java.util.UUID;

public record IbanBlacklistEntry(
    UUID id,
    UUID tenantId,
    String ibanHash,
    String reason,
    int linkedAccountCount,
    boolean active,
    UUID addedBy,
    LocalDateTime addedAt
) {}
