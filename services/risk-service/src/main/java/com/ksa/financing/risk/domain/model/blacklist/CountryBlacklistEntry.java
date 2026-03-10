package com.ksa.financing.risk.domain.model.blacklist;

import java.time.LocalDateTime;
import java.util.UUID;

public record CountryBlacklistEntry(
    UUID id,
    UUID tenantId,
    String countryCode,
    String countryName,
    String countryNameAr,
    String reason,
    boolean active,
    UUID addedBy,
    LocalDateTime addedAt
) {}
