package com.ksa.financing.onboarding.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReferenceDataResponse(
    UUID id,
    String code,
    String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    boolean isActive,
    int displayOrder,
    Instant createdAt,
    Instant updatedAt
) {}
