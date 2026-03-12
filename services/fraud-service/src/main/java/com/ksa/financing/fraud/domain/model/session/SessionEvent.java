package com.ksa.financing.fraud.domain.model.session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionEvent(
    UUID id,
    UUID tenantId,
    String customerId,
    String sessionId,
    String deviceId,
    String ipAddress,
    BigDecimal latitude,
    BigDecimal longitude,
    String country,
    String city,
    LocalDateTime loginAt
) {}
