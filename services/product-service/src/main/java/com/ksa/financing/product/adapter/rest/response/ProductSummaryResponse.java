package com.ksa.financing.product.adapter.rest.response;

import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(
    UUID id,
    String productCode,
    String nameEn,
    String nameAr,
    String productType,
    String status,
    UUID masterCategoryId,
    UUID subCategoryId,
    int wizardStep,
    boolean wizardCompleted,
    Instant createdAt
) {}
