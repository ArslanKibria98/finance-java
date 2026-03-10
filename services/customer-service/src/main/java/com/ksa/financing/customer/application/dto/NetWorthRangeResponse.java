package com.ksa.financing.customer.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NetWorthRangeResponse(
    UUID id,
    String code,
    String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    BigDecimal minValue,
    BigDecimal maxValue,
    boolean isActive,
    int displayOrder,
    Instant createdAt,
    Instant updatedAt
) {}
