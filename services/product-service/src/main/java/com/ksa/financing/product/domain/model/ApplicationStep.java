package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record ApplicationStep(
    UUID id,
    int stepNumber,
    String titleEn,
    String titleAr,
    String description,
    boolean required,
    int sortOrder
) {}
